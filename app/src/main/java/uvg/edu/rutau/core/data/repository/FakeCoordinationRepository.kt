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

/** Maneja los viajes aceptados que se guardan solo mientras la app está abierta. */
class FakeCoordinationRepository(
    private val store: MockRutaUStore,
) : CoordinationRepository {
    override fun observeCoordination(requestId: String): Flow<Coordination?> =
        store.coordinations.map { coordinations ->
            coordinations.firstOrNull { it.requestId == requestId }
        }

    override suspend fun cancelRide(coordinationId: String) {
        synchronized(store) {
            val coordination = findAcceptedCoordination(coordinationId)
            val related = store.coordinations.value.filter {
                it.driverTripId == coordination.driverTripId && it.rideDate == coordination.rideDate
            }
            val requestIds = related.map(Coordination::requestId).toSet()
            store.requests.value = store.requests.value.map { request ->
                if (request.id in requestIds) request.copy(status = RequestStatus.CANCELLED) else request
            }
            store.trips.value = store.trips.value.map { trip ->
                if (trip.id == coordination.driverTripId) {
                    trip.copy(occupiedSeats = (trip.occupiedSeats - related.size).coerceAtLeast(0))
                } else {
                    trip
                }
            }
            store.coordinations.value = store.coordinations.value.filterNot { it in related }
        }
    }

    override suspend fun cancelParticipation(coordinationId: String) {
        synchronized(store) {
            val coordination = findAcceptedCoordination(coordinationId)
            cancelOneCoordination(coordination)
        }
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
        // Una solicitud pendiente no ocupa plazas.
        return id
    }

    private fun findAcceptedCoordination(coordinationId: String): Coordination {
        val coordination = store.coordinations.value.firstOrNull { it.id == coordinationId }
            ?: error("El viaje coordinado no existe.")
        val request = store.requests.value.firstOrNull { it.id == coordination.requestId }
            ?: error("La solicitud no existe.")
        check(request.status == RequestStatus.ACCEPTED) { "Solo los viajes aceptados se pueden cancelar." }
        return coordination
    }

    private fun cancelOneCoordination(coordination: Coordination) {
        store.requests.value = store.requests.value.map { request ->
            if (request.id == coordination.requestId) request.copy(status = RequestStatus.CANCELLED) else request
        }
        store.trips.value = store.trips.value.map { trip ->
            if (trip.id == coordination.driverTripId) {
                trip.copy(occupiedSeats = (trip.occupiedSeats - 1).coerceAtLeast(0))
            } else {
                trip
            }
        }
        store.coordinations.value = store.coordinations.value.filterNot { it.id == coordination.id }
    }
}
