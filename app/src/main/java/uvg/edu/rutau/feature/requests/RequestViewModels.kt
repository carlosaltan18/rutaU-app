package uvg.edu.rutau.feature.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.RideRequestRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.LiveLocationStatus
import uvg.edu.rutau.core.location.LiveTripLocation
import uvg.edu.rutau.core.location.LiveTripLocationRepository
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.data.repository.driverAndPassengerTripIds

/** Indica si se muestran las solicitudes recibidas o enviadas. */
enum class RequestTab {
    RECEIVED,
    SENT,
}

/** Indica el grupo de solicitudes que se muestra en la lista. */
enum class RequestFilter {
    PENDING,
    HISTORY,
}

/** Guarda la información que muestra la lista de solicitudes. */
data class RequestsUiState(
    val selectedTab: RequestTab = RequestTab.RECEIVED,
    val selectedFilter: RequestFilter = RequestFilter.PENDING,
    val requests: List<RideRequestDetails> = emptyList(),
    val receivedCount: Int = 0,
    val sentCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Maneja las solicitudes que aparecen para la persona que inició sesión. */
class RequestsViewModel(
    private val rideRequestRepository: RideRequestRepository,
    userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val isLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)
    private val tab = MutableStateFlow(
        savedStateHandle.get<String>(SelectedTabKey)?.let(RequestTab::valueOf) ?: RequestTab.RECEIVED,
    )
    private val filter = MutableStateFlow(
        savedStateHandle.get<String>(SelectedFilterKey)
            ?.let { saved -> RequestFilter.entries.firstOrNull { it.name == saved } }
            ?: RequestFilter.PENDING,
    )

    private val requestListState = combine(
        rideRequestRepository.observeRequestDetails(),
        userRepository.observeCurrentUser(),
        tab,
        filter,
    ) { details, user, selectedTab, selectedFilter ->
        val userId = user?.id
        val received = details.filter { it.targetTrip.ownerId == userId }
        val sent = details.filter { it.senderTrip.ownerId == userId }
        val requests = (if (selectedTab == RequestTab.RECEIVED) received else sent).filter { detail ->
            val belongsToSelectedTab = if (selectedTab == RequestTab.RECEIVED) {
                detail.targetTrip.ownerId == userId
            } else {
                detail.senderTrip.ownerId == userId
            }
            val belongsToSelectedFilter = when (selectedFilter) {
                RequestFilter.PENDING -> detail.request.status == RequestStatus.PENDING
                RequestFilter.HISTORY -> detail.request.status != RequestStatus.PENDING &&
                    detail.request.status != RequestStatus.ACCEPTED
            }
            belongsToSelectedTab && belongsToSelectedFilter
        }.sortedByDescending { it.request.rideDate }
        RequestsUiState(
            selectedTab = selectedTab,
            selectedFilter = selectedFilter,
            requests = requests,
            receivedCount = received.count { it.request.status == RequestStatus.PENDING },
            sentCount = sent.count { it.request.status == RequestStatus.PENDING },
        )
    }

    val uiState: StateFlow<RequestsUiState> = combine(
        requestListState,
        isLoading,
        error,
    ) { state, loading, message ->
        state.copy(isLoading = loading, error = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RequestsUiState())

    init {
        retry()
    }

    fun retry() = viewModelScope.launch {
        isLoading.value = true
        error.value = null
        runCatching { rideRequestRepository.refresh() }
            .onFailure { throwable ->
                error.value = throwable.message ?: "No fue posible cargar las solicitudes."
            }
        isLoading.value = false
    }

    fun selectTab(value: RequestTab) {
        tab.value = value
        savedStateHandle[SelectedTabKey] = value.name
    }

    fun selectFilter(value: RequestFilter) {
        filter.value = value
        savedStateHandle[SelectedFilterKey] = value.name
    }

    companion object {
        /** Crea el modelo de esta pantalla con los datos compartidos. */
        fun factory(
            rideRequestRepository: RideRequestRepository,
            userRepository: UserRepository,
        ) = viewModelFactory {
            initializer {
                RequestsViewModel(
                    rideRequestRepository,
                    userRepository,
                    createSavedStateHandle(),
                )
            }
        }

        private const val SelectedTabKey = "requests_selected_tab"
        private const val SelectedFilterKey = "requests_selected_filter"
    }
}

/** Guarda la información que muestra el detalle de una solicitud. */
data class RequestDetailUiState(
    val detail: RideRequestDetails? = null,
    val coordination: Coordination? = null,
    val currentUserId: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Maneja las acciones disponibles en el detalle de una solicitud. */
class RequestDetailViewModel(
    private val requestId: String,
    private val rideRequestRepository: RideRequestRepository,
    coordinationRepository: CoordinationRepository,
    userRepository: UserRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RequestDetailUiState> = combine(
        rideRequestRepository.observeRequestDetail(requestId),
        coordinationRepository.observeCoordination(requestId),
        userRepository.observeCurrentUser(),
        saving,
        error,
    ) { detail, coordination, user, isSaving, message ->
        RequestDetailUiState(detail, coordination, user?.id, isSaving, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RequestDetailUiState())

    fun accept(onAccepted: () -> Unit) = change(onAccepted) { rideRequestRepository.accept(requestId) }

    fun reject() = change { rideRequestRepository.reject(requestId) }

    fun cancel() = change { rideRequestRepository.cancel(requestId) }

    private fun change(onDone: () -> Unit = {}, action: suspend () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                action()
                onDone()
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "No fue posible guardar el cambio."
            }
            saving.value = false
        }
    }

    companion object {
        /** Crea el modelo de esta pantalla con los datos compartidos. */
        fun factory(
            requestId: String,
            rideRequestRepository: RideRequestRepository,
            coordinationRepository: CoordinationRepository,
            userRepository: UserRepository,
        ) = requestViewModelFactory {
            RequestDetailViewModel(
                requestId,
                rideRequestRepository,
                coordinationRepository,
                userRepository,
            )
        }
    }
}

/** Guarda la información que muestra un viaje ya aceptado. */
data class CoordinatedRideUiState(
    val detail: RideRequestDetails? = null,
    val coordination: Coordination? = null,
    val confirmedRequests: List<RideRequestDetails> = emptyList(),
    val currentUserId: String? = null,
    val liveLocation: LiveTripLocation? = null,
    val liveLocationIssue: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Maneja las acciones disponibles para un viaje ya aceptado. */
@OptIn(ExperimentalCoroutinesApi::class)
class CoordinatedRideViewModel(
    requestId: String,
    private val coordinationRepository: CoordinationRepository,
    userRepository: UserRepository,
    private val rideRequestRepository: RideRequestRepository,
    private val liveTripLocationRepository: LiveTripLocationRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val liveCoordination = MutableStateFlow<Coordination?>(null)
    private val rideInfo = combine(
        rideRequestRepository.observeRequestDetail(requestId),
        rideRequestRepository.observeRequestDetails(),
        coordinationRepository.observeCoordination(requestId),
    ) { detail, allDetails, coordination ->
        CoordinatedRideInfo(detail, allDetails, coordination)
    }
    private val activeOccurrenceId = combine(rideInfo, liveCoordination) { info, updatedCoordination ->
        (updatedCoordination ?: info.coordination)
            ?.takeIf { it.liveLocationStatus == LiveLocationStatus.ACTIVE }
            ?.liveOccurrenceId
            .orEmpty()
    }.distinctUntilChanged()
    private val liveLocation = activeOccurrenceId.flatMapLatest(liveTripLocationRepository::observe)
    private val liveLocationIssue = activeOccurrenceId.flatMapLatest(liveTripLocationRepository::observeReadIssue)

    private val coordinatedRideInputs = combine(
        rideInfo,
        userRepository.observeCurrentUser(),
        saving,
        error,
        liveCoordination,
    ) { info, user, isSaving, message, updatedCoordination ->
        CoordinatedRideInputs(info, user?.id, isSaving, message, updatedCoordination)
    }

    val uiState: StateFlow<CoordinatedRideUiState> = combine(
        coordinatedRideInputs,
        liveLocation,
        liveLocationIssue,
    ) { inputs, location, locationIssue ->
        val info = inputs.info
        val updatedCoordination = inputs.updatedCoordination
        val coordination = updatedCoordination ?: info.coordination
        val confirmedRequests = coordination?.let { currentCoordination ->
            info.allDetails.filter { requestDetail ->
                val (driverTripId, _) = requestDetail.request.driverAndPassengerTripIds()
                requestDetail.request.status == RequestStatus.ACCEPTED &&
                    requestDetail.request.rideDate == currentCoordination.rideDate &&
                    (currentCoordination.driverTripId.isBlank() || driverTripId == currentCoordination.driverTripId)
            }
        }.orEmpty()
        CoordinatedRideUiState(
            detail = info.detail,
            coordination = coordination,
            confirmedRequests = confirmedRequests,
            currentUserId = inputs.currentUserId,
            liveLocation = location,
            liveLocationIssue = locationIssue,
            isSaving = inputs.isSaving,
            error = inputs.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoordinatedRideUiState())

    fun cancelRide(onCancelled: () -> Unit) {
        val coordinationId = uiState.value.coordination?.id ?: return
        cancel(onCancelled) { coordinationRepository.cancelRide(coordinationId) }
    }

    fun cancelParticipation(onCancelled: () -> Unit) {
        val coordinationId = uiState.value.coordination?.id ?: return
        cancel(onCancelled) {
            coordinationRepository.cancelParticipation(coordinationId)
        }
    }

    /** Guarda una nueva propuesta para reunirse antes del viaje. */
    fun updateMeetingPoint(meetingPoint: String) {
        val coordinationId = uiState.value.coordination?.id ?: return
        if (meetingPoint.trim().length < 3) {
            error.value = "Escribe un punto de encuentro de al menos 3 caracteres."
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                liveCoordination.value = coordinationRepository.updateMeetingPoint(
                    coordinationId,
                    meetingPoint,
                )
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "No fue posible guardar el punto de encuentro."
            }
            saving.value = false
        }
    }

    /** Confirma el punto de encuentro que propuso la otra persona. */
    fun confirmMeetingPoint() {
        val coordinationId = uiState.value.coordination?.id ?: return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                liveCoordination.value = coordinationRepository.confirmMeetingPoint(coordinationId)
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "No fue posible confirmar el punto de encuentro."
            }
            saving.value = false
        }
    }

    /** Explica por qué no se puede iniciar la ubicación si se niega el permiso. */
    fun locationPermissionDenied() {
        error.value = "RutaU necesita permiso de ubicación para compartir tu posición durante el viaje."
    }

    /** Inicia la ubicación únicamente cuando el servidor confirmó que es conductor. */
    fun startLiveLocation(onStarted: (String) -> Unit) {
        val coordinationId = uiState.value.coordination?.id ?: return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                val updated = coordinationRepository.startLiveLocation(coordinationId)
                liveCoordination.value = updated
                updated.liveOccurrenceId?.let(onStarted)
                    ?: error("El servidor no entregó la ocurrencia del viaje.")
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "No fue posible iniciar el viaje."
            }
            saving.value = false
        }
    }

    /** Finaliza el viaje y deja de compartir la ubicación. */
    fun endLiveLocation(onEnded: () -> Unit) {
        val coordinationId = uiState.value.coordination?.id ?: return
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                liveCoordination.value = coordinationRepository.endLiveLocation(coordinationId)
                runCatching { rideRequestRepository.refresh() }
                onEnded()
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "No fue posible finalizar el viaje."
            }
            saving.value = false
        }
    }

    private fun cancel(onCancelled: () -> Unit, action: suspend () -> Coordination) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                liveCoordination.value = action()
                runCatching { rideRequestRepository.refresh() }
                onCancelled()
            } catch (throwable: Throwable) {
                error.value = throwable.message ?: "No fue posible cancelar el viaje."
            }
            saving.value = false
        }
    }

    companion object {
        /** Crea el modelo de esta pantalla con los datos compartidos. */
        fun factory(
            requestId: String,
            coordinationRepository: CoordinationRepository,
            userRepository: UserRepository,
            rideRequestRepository: RideRequestRepository,
            liveTripLocationRepository: LiveTripLocationRepository,
        ) = requestViewModelFactory {
            CoordinatedRideViewModel(
                requestId,
                coordinationRepository,
                userRepository,
                rideRequestRepository,
                liveTripLocationRepository,
            )
        }
    }
}

/** Agrupa los datos necesarios para mostrar un viaje aceptado. */
private data class CoordinatedRideInfo(
    val detail: RideRequestDetails?,
    val allDetails: List<RideRequestDetails>,
    val coordination: Coordination?,
)

/** Mantiene los flujos del viaje dentro del límite tipado de combine. */
private data class CoordinatedRideInputs(
    val info: CoordinatedRideInfo,
    val currentUserId: String?,
    val isSaving: Boolean,
    val error: String?,
    val updatedCoordination: Coordination?,
)

/** Resuelve una coordinación antigua hacia el viaje que le corresponde. */
class CoordinationRedirectViewModel(
    coordinationId: String,
    coordinationRepository: CoordinationRepository,
) : ViewModel() {
    val requestId: StateFlow<String?> = coordinationRepository
        .observeCoordinationById(coordinationId)
        .map { coordination -> coordination?.requestId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(
            coordinationId: String,
            coordinationRepository: CoordinationRepository,
        ) = requestViewModelFactory {
            CoordinationRedirectViewModel(coordinationId, coordinationRepository)
        }
    }
}

/** Crea modelos de pantalla sin depender de una biblioteca adicional. */
private inline fun <reified T : ViewModel> requestViewModelFactory(
    crossinline create: () -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
