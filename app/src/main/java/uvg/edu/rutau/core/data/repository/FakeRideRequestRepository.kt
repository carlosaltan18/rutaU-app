package uvg.edu.rutau.core.data.repository

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.model.Student
import uvg.edu.rutau.core.model.TripRole

/** Maneja las solicitudes de ejemplo que se guardan solo mientras la app está abierta. */
class FakeRideRequestRepository(
    private val store: MockRutaUStore,
) : RideRequestRepository {
    override fun observeRequests(): Flow<List<RideRequest>> = store.requests

    override fun observeRequest(requestId: String): Flow<RideRequest?> =
        store.requests.map { requests -> requests.firstOrNull { it.id == requestId } }

    override fun observeRequestDetails(): Flow<List<RideRequestDetails>> =
        combine(store.requests, store.trips, store.students, store.currentUser) { requests, trips, students, user ->
            val people = students + listOfNotNull(user?.toStudent())
            requests.mapNotNull { request ->
                val senderTrip = trips.firstOrNull { it.id == request.senderTripId }
                val targetTrip = trips.firstOrNull { it.id == request.targetTripId }
                val sender = people.firstOrNull { it.id == senderTrip?.ownerId }
                val target = people.firstOrNull { it.id == targetTrip?.ownerId }
                if (senderTrip != null && targetTrip != null && sender != null && target != null) {
                    RideRequestDetails(request, senderTrip, targetTrip, sender, target)
                } else {
                    null
                }
            }
        }

    override fun observeRequestDetail(requestId: String): Flow<RideRequestDetails?> =
        observeRequestDetails().map { details -> details.firstOrNull { it.request.id == requestId } }

    override suspend fun accept(requestId: String) {
        synchronized(store) {
            val request = requirePendingRequest(requestId)
            val (driverTripId, passengerTripId) = request.driverAndPassengerTripIds()
            val driver = store.trips.value.firstOrNull { it.id == driverTripId }
                ?: error("El trayecto del conductor no existe.")
            require(driver.role == TripRole.DRIVER)
            check(driver.active) { "Un trayecto inactivo no puede aceptar solicitudes." }
            check(driver.availableSeats > 0) { "Este trayecto no tiene plazas disponibles." }

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
            ?: error("La solicitud no existe.")
        check(request.status == RequestStatus.PENDING) { "Solo las solicitudes pendientes pueden cambiar." }
        return request
    }
}

/** Convierte los datos privados en los datos que se pueden mostrar. */
private fun uvg.edu.rutau.core.model.UserAccount.toStudent() = Student(
    id = id,
    fullName = fullName,
    university = university,
    campus = campus,
    photoUrl = photoUrl,
)

/** Indica cuál trayecto conduce y cuál viaja como pasajero. */
internal fun RideRequest.driverAndPassengerTripIds(): Pair<String, String> = when (type) {
    RequestType.JOIN_REQUEST -> targetTripId to senderTripId
    RequestType.DRIVER_INVITATION -> senderTripId to targetTripId
}
