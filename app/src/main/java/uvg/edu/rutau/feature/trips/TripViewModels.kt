package uvg.edu.rutau.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uvg.edu.rutau.core.data.catalog.CatalogItem
import uvg.edu.rutau.core.data.catalog.CatalogRepository
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.FavoriteTripRepository
import uvg.edu.rutau.core.data.repository.TripRepository
import uvg.edu.rutau.core.data.repository.UserRepository
import uvg.edu.rutau.core.data.repository.RideRequestRepository
import uvg.edu.rutau.core.data.repository.driverAndPassengerTripIds
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.CoordinationStatus
import uvg.edu.rutau.core.model.FavoriteTrip
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripInput
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole
import uvg.edu.rutau.core.network.BackendApiException
import uvg.edu.rutau.core.network.nextTravelDate

/** Guarda los trayectos que se muestran en la lista principal. */
data class TripsUiState(
    val trips: List<Trip> = emptyList(),
    val coordinatedTrips: List<CoordinatedTripSummary> = emptyList(),
    val favorites: List<FavoriteTrip> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Resume un viaje coordinado para abrirlo directamente desde Trayectos. */
data class CoordinatedTripSummary(
    val requestId: String,
    val driverTrip: Trip,
    val driver: Student,
    val passengers: List<Student>,
    val rideDate: LocalDate,
    val status: CoordinationStatus,
)

/** Maneja la lista de trayectos de la persona actual. */
class TripsViewModel(
    private val repository: TripRepository,
    rideRequestRepository: RideRequestRepository,
    private val favoriteTripRepository: FavoriteTripRepository,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val trips = MutableStateFlow<List<Trip>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private var tripsLoadJob: Job? = null

    init {
        loadTrips()
    }

    val uiState: StateFlow<TripsUiState> = combine(
        trips,
        rideRequestRepository.observeRequestDetails(),
        favoriteTripRepository.observeFavorites(),
        error,
        isLoading,
    ) { loadedTrips, requests, favorites, message, loading ->
        TripsUiState(
            trips = loadedTrips.map { trip -> trip.withOccupiedSeats(requests) },
            coordinatedTrips = requests.toCoordinatedTripSummaries(),
            favorites = favorites,
            isLoading = loading,
            error = message,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState())

    private fun loadTrips() {
        tripsLoadJob?.cancel()
        tripsLoadJob = viewModelScope.launch {
            isLoading.value = true
            error.value = null
            repository.observeTrips()
                .catch { throwable ->
                    error.value = throwable.message ?: "No fue posible cargar los trayectos."
                    isLoading.value = false
                }
                .collect { loadedTrips ->
                    trips.value = loadedTrips
                    isLoading.value = false
                }
        }
    }

    fun retry() = loadTrips()

    fun remove(tripId: String) = viewModelScope.launch {
        runCatching { repository.deleteTrip(tripId) }
            .onSuccess { error.value = null }
            .onFailure { error.value = it.message ?: "No fue posible eliminar el trayecto." }
    }

    fun deactivate(tripId: String) = viewModelScope.launch {
        runCatching { repository.deactivateTrip(tripId) }
            .onSuccess { error.value = null }
            .onFailure { error.value = it.message ?: "No fue posible desactivar el trayecto." }
    }

    fun removeFavorite(tripId: String) = viewModelScope.launch {
        runCatching { favoriteTripRepository.remove(tripId) }
            .onFailure { error.value = "No fue posible quitar el perfil guardado." }
    }

    companion object {
        fun factory(
            repository: TripRepository,
            rideRequestRepository: RideRequestRepository,
            favoriteTripRepository: FavoriteTripRepository,
        ) = viewModelFactory { TripsViewModel(repository, rideRequestRepository, favoriteTripRepository) }
    }
}

/** Guarda los datos que se editan en el formulario de un trayecto. */
data class TripEditorUiState(
    val originZone: String = "",
    val destinationCampus: String = "",
    val zones: List<CatalogItem> = emptyList(),
    val campuses: List<CatalogItem> = emptyList(),
    val dayOfWeek: String = "Lunes",
    val departureTime: LocalTime = LocalTime.of(6, 30),
    val role: TripRole = TripRole.PASSENGER,
    val offeredSeats: Int = 1,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Maneja la creación y edición de un trayecto. */
class TripEditorViewModel(
    private val tripId: String?,
    private val repository: TripRepository,
    private val catalogRepository: CatalogRepository? = null,
    private val userRepository: UserRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripEditorUiState(isEditing = tripId != null))
    val uiState = _uiState.asStateFlow()

    init {
        loadCatalogs()
        if (tripId != null) {
            viewModelScope.launch {
                val trip = repository.observeTrip(tripId).filterNotNull().first()
                update {
                    copy(
                        originZone = trip.originZoneId,
                        destinationCampus = trip.destinationCampusId,
                        dayOfWeek = trip.dayOfWeek,
                        departureTime = trip.departureTime,
                        role = trip.role,
                        offeredSeats = trip.offeredSeats.coerceAtLeast(1),
                        isEditing = true,
                    )
                }
            }
        }
    }

    fun setOrigin(value: String) = update { copy(originZone = value, error = null) }
    fun setCampus(value: String) = update { copy(destinationCampus = value, error = null) }
    fun setDay(value: String) = update { copy(dayOfWeek = value, error = null) }
    fun setTime(value: LocalTime) = update { copy(departureTime = value, error = null) }
    fun setRole(value: TripRole) = update { copy(role = value, error = null) }
    fun setSeats(value: Int) = update { copy(offeredSeats = value.coerceIn(1, 3), error = null) }

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        if (state.originZone.isBlank()) {
            _uiState.value = state.copy(error = "Selecciona una zona de origen.")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            runCatching {
                val input = TripInput(
                    originZone = state.originZone,
                    destinationCampus = state.destinationCampus,
                    dayOfWeek = state.dayOfWeek,
                    departureTime = state.departureTime,
                    role = state.role,
                    offeredSeats = if (state.role == TripRole.DRIVER) state.offeredSeats else 0,
                )
                if (tripId == null) repository.createTrip(input) else {
                    repository.updateTrip(tripId, input)
                    tripId
                }
            }.onSuccess(onSaved).onFailure { error ->
                _uiState.value = state.copy(
                    isSaving = false,
                    error = error.message ?: "No fue posible guardar el trayecto.",
                )
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = tripId ?: return
        viewModelScope.launch {
            runCatching { repository.deleteTrip(id) }
                .onSuccess { onDeleted() }
                .onFailure { error ->
                    update { copy(error = error.message ?: "No fue posible eliminar el trayecto.") }
                }
        }
    }

    private fun update(transform: TripEditorUiState.() -> TripEditorUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun loadCatalogs() {
        val catalogs = catalogRepository ?: return
        viewModelScope.launch {
            runCatching { catalogs.zones() }
                .onSuccess { zones -> update { copy(zones = zones) } }
                .onFailure { update { copy(error = "No fue posible cargar las zonas.") } }
            val universityId = userRepository?.observeCurrentUser()?.first()?.universityId.orEmpty()
            if (universityId.isNotBlank()) {
                runCatching { catalogs.campuses(universityId) }
                    .onSuccess { campuses -> update { copy(campuses = campuses) } }
                    .onFailure { update { copy(error = "No fue posible cargar los campus.") } }
            }
        }
    }

    companion object {
        fun factory(
            tripId: String?,
            repository: TripRepository,
            catalogRepository: CatalogRepository? = null,
            userRepository: UserRepository? = null,
        ) = viewModelFactory { TripEditorViewModel(tripId, repository, catalogRepository, userRepository) }
    }
}

/** Guarda el trayecto elegido y sus personas compatibles. */
data class MatchesUiState(
    val sourceTrip: Trip? = null,
    val matches: List<TripMatch> = emptyList(),
)

/** Busca personas compatibles para un trayecto. */
class MatchesViewModel(
    tripId: String,
    repository: TripRepository,
    rideRequestRepository: RideRequestRepository,
) : ViewModel() {
    val uiState = combine(
        repository.observeTrip(tripId),
        repository.observeMatches(tripId),
        rideRequestRepository.observeRequestDetails(),
    ) { trip, matches, requests ->
        val hiddenCandidateIds = trip?.let { source ->
            requests.asSequence()
                .filter { detail -> detail.blocksNewRequestFor(source.id, source.dayOfWeek) }
                .map { detail ->
                    if (detail.request.senderTripId == source.id) {
                        detail.request.targetTripId
                    } else {
                        detail.request.senderTripId
                    }
                }
                .toSet()
        }.orEmpty()
        MatchesUiState(trip, matches.filterNot { it.trip.id in hiddenCandidateIds })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchesUiState())

    companion object {
        fun factory(
            tripId: String,
            repository: TripRepository,
            rideRequestRepository: RideRequestRepository,
        ) = viewModelFactory { MatchesViewModel(tripId, repository, rideRequestRepository) }
    }
}

/** Guarda los datos que se muestran en el perfil compatible. */
data class CandidateProfileUiState(
    val sourceTrip: Trip? = null,
    val candidateTrip: Trip? = null,
    val student: Student? = null,
    val contributionQuetzales: Int = 10,
    val existingRequestId: String? = null,
    val existingRequestStatus: RequestStatus? = null,
    val isFavorite: Boolean = false,
)

/** Maneja la información de una persona compatible. */
class CandidateProfileViewModel(
    tripId: String,
    candidateTripId: String,
    repository: TripRepository,
    requestRepository: RideRequestRepository,
    private val favoriteTripRepository: FavoriteTripRepository,
) : ViewModel() {
    private val contribution = MutableStateFlow(10)
    val uiState = combine(
        repository.observeTrip(tripId),
        repository.observeMatches(tripId),
        contribution,
        requestRepository.observeRequestDetails(),
        favoriteTripRepository.observeFavoriteTripIds(),
    ) { source, matches, amount, requests, favoriteTripIds ->
        val match = matches.firstOrNull { it.trip.id == candidateTripId }
        val existing = match?.let { candidate ->
            requests.firstOrNull { detail ->
                source != null &&
                    detail.blocksNewRequestFor(source.id, source.dayOfWeek) &&
                    (detail.request.senderTripId == candidate.trip.id ||
                        detail.request.targetTripId == candidate.trip.id)
            }
        }
        CandidateProfileUiState(
            sourceTrip = source,
            candidateTrip = match?.trip,
            student = match?.student,
            contributionQuetzales = amount,
            existingRequestId = existing?.request?.id,
            existingRequestStatus = existing?.request?.status,
            isFavorite = match?.trip?.id?.let(favoriteTripIds::contains) == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CandidateProfileUiState())

    fun setContribution(value: Int) { contribution.value = value.coerceIn(0, 100) }

    fun toggleFavorite() = viewModelScope.launch {
        val state = uiState.value
        val trip = state.candidateTrip ?: return@launch
        val student = state.student ?: return@launch
        if (state.isFavorite) {
            favoriteTripRepository.remove(trip.id)
        } else {
            favoriteTripRepository.save(
                FavoriteTrip(
                    tripId = trip.id,
                    studentName = student.fullName,
                    studentPhotoUrl = student.photoUrl,
                    routeDescription = "${trip.originZone} → ${trip.destinationCampus}",
                    scheduleDescription = "${trip.dayOfWeek} · ${trip.departureTime}",
                    savedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    companion object {
        fun factory(
            tripId: String,
            candidateTripId: String,
            repository: TripRepository,
            requestRepository: RideRequestRepository,
            favoriteTripRepository: FavoriteTripRepository,
        ) = viewModelFactory {
            CandidateProfileViewModel(
                tripId,
                candidateTripId,
                repository,
                requestRepository,
                favoriteTripRepository,
            )
        }
    }
}

/** Guarda los datos para enviar una solicitud o invitación. */
data class ConfirmCoordinationUiState(
    val sourceTrip: Trip? = null,
    val candidateTrip: Trip? = null,
    val student: Student? = null,
    val message: String = "",
    val contributionQuetzales: Int = 10,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

/** Maneja el envío de una solicitud o invitación. */
class ConfirmCoordinationViewModel(
    private val tripId: String,
    private val candidateTripId: String,
    initialContribution: Int,
    tripRepository: TripRepository,
    private val coordinationRepository: CoordinationRepository,
) : ViewModel() {
    private val form = MutableStateFlow(
        ConfirmCoordinationUiState(contributionQuetzales = initialContribution.coerceIn(0, 100)),
    )
    val uiState = combine(
        tripRepository.observeTrip(tripId),
        tripRepository.observeMatches(tripId),
        form,
    ) { source, matches, current ->
        val match = matches.firstOrNull { it.trip.id == candidateTripId }
        current.copy(sourceTrip = source, candidateTrip = match?.trip, student = match?.student)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfirmCoordinationUiState())

    fun setMessage(value: String) { form.value = form.value.copy(message = value, error = null) }
    fun setContribution(value: Int) {
        form.value = form.value.copy(contributionQuetzales = value.coerceIn(0, 100), error = null)
    }

    fun confirm(onConfirmed: (String) -> Unit) {
        val state = uiState.value
        val source = state.sourceTrip ?: return
        viewModelScope.launch {
            form.value = state.copy(isSubmitting = true, error = null)
            runCatching {
                coordinationRepository.createPendingCoordination(
                    tripId = tripId,
                    candidateTripId = candidateTripId,
                    type = if (source.role == TripRole.PASSENGER) {
                        RequestType.JOIN_REQUEST
                    } else {
                        RequestType.DRIVER_INVITATION
                    },
                    message = state.message,
                    contributionCents = state.contributionQuetzales * 100L,
                )
            }.onSuccess(onConfirmed).onFailure { error ->
                form.value = state.copy(
                    isSubmitting = false,
                    error = error.asCoordinationError(),
                )
            }
        }
    }

    companion object {
        fun factory(
            tripId: String,
            candidateTripId: String,
            initialContribution: Int = 10,
            tripRepository: TripRepository,
            coordinationRepository: CoordinationRepository,
        ) = viewModelFactory {
            ConfirmCoordinationViewModel(
                tripId,
                candidateTripId,
                initialContribution,
                tripRepository,
                coordinationRepository,
            )
        }
    }
}

/** Calcula las plazas ya confirmadas para la próxima salida del conductor. */
private fun Trip.withOccupiedSeats(requests: List<uvg.edu.rutau.core.model.RideRequestDetails>): Trip {
    if (role != TripRole.DRIVER) return copy(occupiedSeats = 0)
    val nextDate = nextTravelDate(dayOfWeek)
    val occupied = requests.count { detail ->
        val (driverTripId, _) = detail.request.driverAndPassengerTripIds()
        driverTripId == id &&
            detail.request.rideDate.toString() == nextDate &&
            detail.request.status == RequestStatus.ACCEPTED &&
            detail.coordinationStatus == uvg.edu.rutau.core.model.CoordinationStatus.CONFIRMED
    }
    return copy(occupiedSeats = occupied.coerceAtMost(offeredSeats))
}

/** Agrupa a los pasajeros de un mismo viaje para no repetirlo en la lista. */
private fun List<RideRequestDetails>.toCoordinatedTripSummaries(): List<CoordinatedTripSummary> =
    filter { detail ->
        detail.request.status == RequestStatus.ACCEPTED && detail.coordinationStatus != null
    }
        .groupBy { detail ->
            val (driverTripId, _) = detail.request.driverAndPassengerTripIds()
            Triple(
                driverTripId,
                detail.request.rideDate,
                requireNotNull(detail.coordinationStatus),
            )
        }
        .mapNotNull { (_, details) ->
            val first = details.firstOrNull() ?: return@mapNotNull null
            val driverIsSender = first.senderTrip.role == TripRole.DRIVER
            val driverTrip = if (driverIsSender) first.senderTrip else first.targetTrip
            val driver = if (driverIsSender) first.sender else first.target
            val passengers = details.map { detail ->
                if (detail.senderTrip.role == TripRole.PASSENGER) detail.sender else detail.target
            }.distinctBy(Student::id)
            CoordinatedTripSummary(
                requestId = first.request.id,
                driverTrip = driverTrip,
                driver = driver,
                passengers = passengers,
                rideDate = first.request.rideDate,
                status = requireNotNull(first.coordinationStatus),
            )
        }
        .sortedWith(
            compareBy<CoordinatedTripSummary> {
                if (it.status == CoordinationStatus.CONFIRMED) 0 else 1
            }.thenByDescending { it.rideDate },
        )

/** Decide si una solicitud de la misma fecha evita enviar otra a esa persona. */
private fun uvg.edu.rutau.core.model.RideRequestDetails.blocksNewRequestFor(
    sourceTripId: String,
    dayOfWeek: String,
): Boolean {
    val rideRequest = request
    val includesSource = rideRequest.senderTripId == sourceTripId || rideRequest.targetTripId == sourceTripId
    if (!includesSource || rideRequest.rideDate.toString() != nextTravelDate(dayOfWeek)) return false
    return rideRequest.status == RequestStatus.PENDING ||
        (rideRequest.status == RequestStatus.ACCEPTED &&
            coordinationStatus == uvg.edu.rutau.core.model.CoordinationStatus.CONFIRMED)
}

/** Convierte un error temporal del servidor en un mensaje claro para la persona. */
private fun Throwable.asCoordinationError(): String = when {
    this is BackendApiException && code == "RATE_LIMITED" ->
        "Hay muchas solicitudes en este momento. Espera unos segundos e inténtalo de nuevo."
    else -> message ?: "No fue posible enviar la coordinación."
}

private inline fun <reified T : ViewModel> viewModelFactory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
