package uvg.edu.rutau.core.data.repository

import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest

class FakeCoordinationRepository(
    private val store: MockRutaUStore,
) : CoordinationRepository {
    override fun observeCoordination(requestId: String): Flow<Coordination?> =
        store.coordinations.map { coordinations ->
            coordinations.firstOrNull { it.requestId == requestId }
        }

    override suspend fun cancelRide(coordinationId: String) {
        cancelConfirmedCoordination(coordinationId)
    }

    override suspend fun cancelParticipation(coordinationId: String) {
        cancelConfirmedCoordination(coordinationId)
    }

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

    private fun cancelConfirmedCoordination(coordinationId: String) {
        synchronized(store) {
            val coordination = store.coordinations.value.firstOrNull { it.id == coordinationId }
                ?: error("Coordination does not exist.")
            val request = store.requests.value.firstOrNull { it.id == coordination.requestId }
                ?: error("Ride request does not exist.")
            check(request.status == RequestStatus.ACCEPTED) { "Only accepted coordinations can be cancelled." }

            store.requests.value = store.requests.value.map { existing ->
                if (existing.id == request.id) existing.copy(status = RequestStatus.CANCELLED) else existing
            }
            store.trips.value = store.trips.value.map { trip ->
                if (trip.id == coordination.driverTripId) {
                    trip.copy(occupiedSeats = (trip.occupiedSeats - 1).coerceAtLeast(0))
                } else {
                    trip
                }
            }
            store.coordinations.value = store.coordinations.value.filterNot { it.id == coordinationId }
        }
    }
}
