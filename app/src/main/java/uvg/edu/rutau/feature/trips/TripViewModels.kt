package uvg.edu.rutau.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uvg.edu.rutau.core.data.repository.CoordinationRepository
import uvg.edu.rutau.core.data.repository.TripRepository
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.Trip
import uvg.edu.rutau.core.model.TripInput
import uvg.edu.rutau.core.model.TripMatch
import uvg.edu.rutau.core.model.TripRole

data class TripsUiState(val trips: List<Trip> = emptyList())

class TripsViewModel(private val repository: TripRepository) : ViewModel() {
    val uiState: StateFlow<TripsUiState> = repository.observeTrips()
        .combine(MutableStateFlow(Unit)) { trips, _ -> TripsUiState(trips) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState())

    fun remove(tripId: String) = viewModelScope.launch { repository.deleteTrip(tripId) }
    fun deactivate(tripId: String) = viewModelScope.launch { repository.deactivateTrip(tripId) }

    companion object {
        fun factory(repository: TripRepository) = viewModelFactory { TripsViewModel(repository) }
    }
}

data class TripEditorUiState(
    val originZone: String = "Zona 11",
    val destinationCampus: String = "Campus Central",
    val dayOfWeek: String = "Lunes",
    val departureTime: LocalTime = LocalTime.of(6, 30),
    val role: TripRole = TripRole.PASSENGER,
    val offeredSeats: Int = 1,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

class TripEditorViewModel(
    private val tripId: String?,
    private val repository: TripRepository,
) : ViewModel() {
    val uiState = MutableStateFlow(TripEditorUiState(isEditing = tripId != null))

    init {
        if (tripId != null) {
            viewModelScope.launch {
                val trip = repository.observeTrip(tripId).filterNotNull().first()
                uiState.value = TripEditorUiState(
                    originZone = trip.originZone,
                    destinationCampus = trip.destinationCampus,
                    dayOfWeek = trip.dayOfWeek,
                    departureTime = trip.departureTime,
                    role = trip.role,
                    offeredSeats = trip.offeredSeats.coerceAtLeast(1),
                    isEditing = true,
                )
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
        val state = uiState.value
        if (state.originZone.isBlank()) {
            uiState.value = state.copy(error = "Selecciona una zona de origen.")
            return
        }
        viewModelScope.launch {
            uiState.value = state.copy(isSaving = true, error = null)
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
            }.onSuccess(onSaved).onFailure {
                uiState.value = state.copy(error = "No fue posible guardar el trayecto.")
            }
        }
    }

    private fun update(transform: TripEditorUiState.() -> TripEditorUiState) {
        uiState.value = uiState.value.transform()
    }

    companion object {
        fun factory(tripId: String?, repository: TripRepository) =
            viewModelFactory { TripEditorViewModel(tripId, repository) }
    }
}

data class MatchesUiState(
    val sourceTrip: Trip? = null,
    val matches: List<TripMatch> = emptyList(),
)

class MatchesViewModel(tripId: String, repository: TripRepository) : ViewModel() {
    val uiState = combine(repository.observeTrip(tripId), repository.observeMatches(tripId)) { trip, matches ->
        MatchesUiState(trip, matches)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchesUiState())

    companion object {
        fun factory(tripId: String, repository: TripRepository) =
            viewModelFactory { MatchesViewModel(tripId, repository) }
    }
}

data class CandidateProfileUiState(
    val sourceTrip: Trip? = null,
    val candidateTrip: Trip? = null,
    val student: Student? = null,
    val contributionQuetzales: Int = 10,
)

class CandidateProfileViewModel(
    tripId: String,
    candidateTripId: String,
    repository: TripRepository,
) : ViewModel() {
    private val contribution = MutableStateFlow(10)
    val uiState = combine(
        repository.observeTrip(tripId),
        repository.observeMatches(tripId),
        contribution,
    ) { source, matches, amount ->
        val match = matches.firstOrNull { it.trip.id == candidateTripId }
        CandidateProfileUiState(source, match?.trip, match?.student, amount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CandidateProfileUiState())

    fun setContribution(value: Int) { contribution.value = value.coerceIn(0, 100) }

    companion object {
        fun factory(tripId: String, candidateTripId: String, repository: TripRepository) =
            viewModelFactory { CandidateProfileViewModel(tripId, candidateTripId, repository) }
    }
}

data class ConfirmCoordinationUiState(
    val sourceTrip: Trip? = null,
    val candidateTrip: Trip? = null,
    val student: Student? = null,
    val message: String = "",
    val contributionQuetzales: Int = 10,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

class ConfirmCoordinationViewModel(
    private val tripId: String,
    private val candidateTripId: String,
    tripRepository: TripRepository,
    private val coordinationRepository: CoordinationRepository,
) : ViewModel() {
    private val form = MutableStateFlow(ConfirmCoordinationUiState())
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
            }.onSuccess(onConfirmed).onFailure {
                form.value = state.copy(error = "No fue posible enviar la coordinación.")
            }
        }
    }

    companion object {
        fun factory(
            tripId: String,
            candidateTripId: String,
            tripRepository: TripRepository,
            coordinationRepository: CoordinationRepository,
        ) = viewModelFactory {
            ConfirmCoordinationViewModel(tripId, candidateTripId, tripRepository, coordinationRepository)
        }
    }
}

private inline fun <reified T : ViewModel> viewModelFactory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
