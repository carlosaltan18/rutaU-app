package uvg.edu.rutau.core.data.repository

import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.data.mock.MockRutaUStore
import uvg.edu.rutau.core.model.CancellationReason
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.CoordinationStatus
import uvg.edu.rutau.core.model.MeetingPointInput
import uvg.edu.rutau.core.model.RequestStatus
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.model.RideRequest

/** Maneja los viajes aceptados que se guardan solo mientras la app está abierta. */
class FakeCoordinationRepository(
    private val store: MockRutaUStore,
) : CoordinationRepository {
    override fun observeCoordinations(): Flow<List<Coordination>> = store.coordinations

    override fun observeCoordination(requestId: String): Flow<Coordination?> =
        store.coordinations.map { coordinations ->
            coordinations.firstOrNull { it.requestId == requestId }
        }

    override fun observeCoordinationById(coordinationId: String): Flow<Coordination?> =
        store.coordinations.map { coordinations -> coordinations.firstOrNull { it.id == coordinationId } }

    override suspend fun cancelRide(
        coordinationId: String,
        reason: CancellationReason,
    ): Coordination {
        return synchronized(store) {
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
            store.coordinations.value = store.coordinations.value.map { current ->
                if (current in related) {
                    current.copy(
                        status = CoordinationStatus.CANCELLED,
                        cancellationReason = reason,
                    )
                } else {
                    current
                }
            }
            coordination.copy(status = CoordinationStatus.CANCELLED, cancellationReason = reason)
        }
    }

    override suspend fun cancelParticipation(
        coordinationId: String,
        reason: CancellationReason,
    ): Coordination {
        return synchronized(store) {
            val coordination = findAcceptedCoordination(coordinationId)
            cancelOneCoordination(coordination, reason)
            coordination.copy(status = CoordinationStatus.CANCELLED, cancellationReason = reason)
        }
    }

    override suspend fun markArrival(coordinationId: String): Coordination {
        val current = findAcceptedCoordination(coordinationId)
        val updated = current.copy(arrivedByCurrentUser = true)
        store.coordinations.value = store.coordinations.value.map {
            if (it.id == coordinationId) updated else it
        }
        return updated
    }

    override suspend fun confirmContribution(coordinationId: String): Coordination {
        val current = store.coordinations.value.firstOrNull { it.id == coordinationId }
            ?: error("El viaje coordinado no existe.")
        check(current.status == CoordinationStatus.COMPLETED) {
            "El aporte se confirma después de finalizar el viaje."
        }
        val updated = current.copy(contributionConfirmedByCurrentUser = true)
        store.coordinations.value = store.coordinations.value.map {
            if (it.id == coordinationId) updated else it
        }
        return updated
    }

    override suspend fun updateMeetingPoint(
        coordinationId: String,
        meetingPoint: MeetingPointInput,
    ): Coordination {
        val current = findAcceptedCoordination(coordinationId)
        val updated = current.copy(
            meetingPoint = meetingPoint.label.trim(),
            meetingLatitude = meetingPoint.latitude,
            meetingLongitude = meetingPoint.longitude,
            meetingPointConfirmedByCurrentUser = true,
            meetingPointConfirmedByOtherParticipant = false,
        )
        store.coordinations.value = store.coordinations.value.map {
            if (it.id == coordinationId) updated else it
        }
        return updated
    }

    override suspend fun confirmMeetingPoint(coordinationId: String): Coordination {
        val current = findAcceptedCoordination(coordinationId)
        check(!current.meetingPoint.isNullOrBlank()) { "Primero deben indicar un punto de encuentro." }
        val updated = current.copy(meetingPointConfirmedByCurrentUser = true)
        store.coordinations.value = store.coordinations.value.map {
            if (it.id == coordinationId) updated else it
        }
        return updated
    }

    override suspend fun startLiveLocation(coordinationId: String): Coordination =
        error("La ubicación en vivo requiere una coordinación del servidor.")

    override suspend fun endLiveLocation(coordinationId: String): Coordination =
        error("La ubicación en vivo requiere una coordinación del servidor.")

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

    private fun cancelOneCoordination(
        coordination: Coordination,
        reason: CancellationReason,
    ) {
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
        store.coordinations.value = store.coordinations.value.map { current ->
            if (current.id == coordination.id) {
                current.copy(
                    status = CoordinationStatus.CANCELLED,
                    cancellationReason = reason,
                )
            } else {
                current
            }
        }
    }
}
