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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.RideRequestRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.model.Coordination
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
    ALL,
    PENDING,
    ACCEPTED,
}

/** Guarda la información que muestra la lista de solicitudes. */
data class RequestsUiState(
    val selectedTab: RequestTab = RequestTab.RECEIVED,
    val selectedFilter: RequestFilter = RequestFilter.ALL,
    val requests: List<RideRequestDetails> = emptyList(),
    val receivedCount: Int = 0,
    val sentCount: Int = 0,
)

/** Maneja las solicitudes que aparecen para la persona que inició sesión. */
class RequestsViewModel(
    rideRequestRepository: RideRequestRepository,
    userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val tab = MutableStateFlow(
        savedStateHandle.get<String>(SelectedTabKey)?.let(RequestTab::valueOf) ?: RequestTab.RECEIVED,
    )
    private val filter = MutableStateFlow(
        savedStateHandle.get<String>(SelectedFilterKey)?.let(RequestFilter::valueOf) ?: RequestFilter.ALL,
    )

    val uiState: StateFlow<RequestsUiState> = combine(
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
                RequestFilter.ALL -> true
                RequestFilter.PENDING -> detail.request.status == RequestStatus.PENDING
                RequestFilter.ACCEPTED -> detail.request.status == RequestStatus.ACCEPTED
            }
            belongsToSelectedTab && belongsToSelectedFilter
        }.sortedByDescending { it.request.rideDate }
        RequestsUiState(
            selectedTab = selectedTab,
            selectedFilter = selectedFilter,
            requests = requests,
            receivedCount = received.size,
            sentCount = sent.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RequestsUiState())

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
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Maneja las acciones disponibles para un viaje ya aceptado. */
class CoordinatedRideViewModel(
    requestId: String,
    private val coordinationRepository: CoordinationRepository,
    userRepository: UserRepository,
    rideRequestRepository: RideRequestRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val rideInfo = combine(
        rideRequestRepository.observeRequestDetail(requestId),
        rideRequestRepository.observeRequestDetails(),
        coordinationRepository.observeCoordination(requestId),
    ) { detail, allDetails, coordination ->
        CoordinatedRideInfo(detail, allDetails, coordination)
    }

    val uiState: StateFlow<CoordinatedRideUiState> = combine(
        rideInfo,
        userRepository.observeCurrentUser(),
        saving,
        error,
    ) { info, user, isSaving, message ->
        val confirmedRequests = info.coordination?.let { coordination ->
            info.allDetails.filter { requestDetail ->
                val (driverTripId, _) = requestDetail.request.driverAndPassengerTripIds()
                requestDetail.request.status == RequestStatus.ACCEPTED &&
                    requestDetail.request.rideDate == coordination.rideDate &&
                    driverTripId == coordination.driverTripId
            }
        }.orEmpty()
        CoordinatedRideUiState(
            detail = info.detail,
            coordination = info.coordination,
            confirmedRequests = confirmedRequests,
            currentUserId = user?.id,
            isSaving = isSaving,
            error = message,
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

    private fun cancel(onCancelled: () -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            error.value = null
            try {
                action()
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
        ) = requestViewModelFactory {
            CoordinatedRideViewModel(
                requestId,
                coordinationRepository,
                userRepository,
                rideRequestRepository,
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

/** Crea modelos de pantalla sin depender de una biblioteca adicional. */
private inline fun <reified T : ViewModel> requestViewModelFactory(
    crossinline create: () -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
