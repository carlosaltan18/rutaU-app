package uvg.edu.rutau.core.data.repository

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.TripRole

/** In-memory request state machine shared by the Requests and Trips modules. */
class FakeRideRequestRepository(
    private val store: MockRutaUStore,
) : RideRequestRepository {
    override fun observeRequests(): Flow<List<RideRequest>> = store.requests

    override fun observeRequest(requestId: String): Flow<RideRequest?> =
        store.requests.map { requests -> requests.firstOrNull { it.id == requestId } }

    override suspend fun accept(requestId: String) {
        synchronized(store) {
            val request = requirePendingRequest(requestId)
            val (driverTripId, passengerTripId) = request.driverAndPassengerTripIds()
            val driver = store.trips.value.firstOrNull { it.id == driverTripId }
                ?: error("Driver trip does not exist.")
            require(driver.role == TripRole.DRIVER)
            check(driver.active) { "An inactive trip cannot accept requests." }
            check(driver.availableSeats > 0) { "This trip has no seats available." }

            store.requests.value = store.requests.value.map { existing ->
                if (existing.id == requestId) existing.copy(status = RequestStatus.ACCEPTED) else existing
            }
            store.trips.value = store.trips.value.map { trip ->
                if (trip.id == driverTripId) trip.copy(occupiedSeats = trip.occupiedSeats + 1) else trip
            }
            store.coordinations.value += Coordination(
                id = "coord-${UUID.randomUUID()}",
                requestId = request.id,
                driverTripId = driverTripId,
                passengerTripId = passengerTripId,
                rideDate = request.rideDate,
                contributionCents = request.contributionCents,
            )
        }
    }

    override suspend fun reject(requestId: String) = updatePendingStatus(requestId, RequestStatus.REJECTED)

    override suspend fun cancel(requestId: String) = updatePendingStatus(requestId, RequestStatus.CANCELLED)

    override suspend fun expire(requestId: String) = updatePendingStatus(requestId, RequestStatus.EXPIRED)

    private fun updatePendingStatus(requestId: String, status: RequestStatus) {
        synchronized(store) {
            requirePendingRequest(requestId)
            store.requests.value = store.requests.value.map { existing ->
                if (existing.id == requestId) existing.copy(status = status) else existing
            }
        }
    }

    private fun requirePendingRequest(requestId: String): RideRequest {
        val request = store.requests.value.firstOrNull { it.id == requestId }
            ?: error("Ride request does not exist.")
        check(request.status == RequestStatus.PENDING) { "Only pending requests can change state." }
        return request
    }
}

internal fun RideRequest.driverAndPassengerTripIds(): Pair<String, String> = when (type) {
    RequestType.JOIN_REQUEST -> targetTripId to senderTripId
    RequestType.DRIVER_INVITATION -> senderTripId to targetTripId
}
