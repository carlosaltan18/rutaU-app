package uvg.edu.rutau.core.data.repository

import java.time.LocalDate
import java.util.UUID
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest

class FakeCoordinationRepository(
    private val store: MockRutaUStore,
) : CoordinationRepository {
    override suspend fun createPendingCoordination(
        tripId: String,
        candidateTripId: String,
        type: RequestType,
        message: String?,
        contributionCents: Long,
    ): String {
        require(store.trips.value.any { it.id == tripId })
        require(store.trips.value.any { it.id == candidateTripId })
        require(contributionCents >= 0)
        val id = "request-${UUID.randomUUID()}"
        store.requests.value += RideRequest(
            id = id,
            senderTripId = tripId,
            targetTripId = candidateTripId,
            type = type,
            status = RequestStatus.PENDING,
            rideDate = LocalDate.now().plusDays(1),
            message = message?.trim()?.takeIf(String::isNotEmpty),
            contributionCents = contributionCents,
        )
        // A pending request never reserves or reduces seats.
        return id
    }
}
