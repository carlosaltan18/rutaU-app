package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.BackendApiException
import uvg.edu.rutau.core.network.CreateRideRequestRequestDto
import uvg.edu.rutau.core.network.IdempotencyKeyStore
import uvg.edu.rutau.core.network.UpdateMeetingPointRequestDto
import uvg.edu.rutau.core.network.nextTravelDate
import uvg.edu.rutau.core.network.requireData
import uvg.edu.rutau.core.network.requireEnvelope
import uvg.edu.rutau.core.network.toCoordination

/** Conecta los viajes coordinados con las llamadas reales del servidor. */
class RemoteCoordinationRepository(
    private val backendApi: BackendApi,
) : CoordinationRepository {
    private val idempotencyKeys = IdempotencyKeyStore()
    override fun observeCoordination(requestId: String): Flow<Coordination?> = flow {
        while (currentCoroutineContext().isActive) {
            val coordination = try {
                backendApi.getCoordinationForRequest(requestId).requireData().toCoordination()
            } catch (error: BackendApiException) {
                if (error.code == CoordinationNotFound) null else throw error
            }
            emit(coordination)
            delay(RefreshIntervalMillis)
        }
    }

    override fun observeCoordinationById(coordinationId: String): Flow<Coordination?> = flow {
        while (currentCoroutineContext().isActive) {
            emit(backendApi.getCoordination(coordinationId).requireData().toCoordination())
            delay(RefreshIntervalMillis)
        }
    }

    override suspend fun cancelRide(coordinationId: String): Coordination =
        backendApi.cancelCoordination(coordinationId).requireData().toCoordination()

    override suspend fun cancelParticipation(coordinationId: String): Coordination =
        backendApi.cancelCoordination(coordinationId).requireData().toCoordination()

    override suspend fun updateMeetingPoint(coordinationId: String, meetingPoint: String): Coordination =
        backendApi.updateMeetingPoint(
            coordinationId,
            UpdateMeetingPointRequestDto(meetingPoint.trim()),
        ).requireData().toCoordination()

    override suspend fun confirmMeetingPoint(coordinationId: String): Coordination =
        backendApi.confirmMeetingPoint(coordinationId).requireData().toCoordination()

    override suspend fun startLiveLocation(coordinationId: String): Coordination =
        backendApi.startLiveLocation(coordinationId).requireData().toCoordination()

    override suspend fun endLiveLocation(coordinationId: String): Coordination =
        backendApi.endLiveLocation(coordinationId).requireData().toCoordination()

    override suspend fun createPendingCoordination(
        tripId: String,
        candidateTripId: String,
        type: RequestType,
        message: String?,
        contributionCents: Long,
    ): String {
        val source = backendApi.getTrip(tripId).requireData()
        val travelDate = nextTravelDate(source.dayOfWeek.toDayName())
        val operation = "request:$tripId:$candidateTripId:$type:$contributionCents:${message.orEmpty()}"
        val requestId = try {
            backendApi.createRideRequest(
                idempotencyKey = idempotencyKeys.keyFor(operation),
                body = CreateRideRequestRequestDto(
                    senderTripId = tripId,
                    targetTripId = candidateTripId,
                    type = type.name,
                    travelDate = travelDate,
                    offeredContributionMinor = contributionCents,
                    message = message?.trim()?.takeIf(String::isNotEmpty),
                ),
            ).requireData().id
        } catch (error: BackendApiException) {
            if (error.code != RequestAlreadyExists) throw error
            findExistingRequest(tripId, candidateTripId, type.name, travelDate)
                ?: throw error
        }
        idempotencyKeys.complete(operation)
        return requestId
    }

    private suspend fun findExistingRequest(
        tripId: String,
        candidateTripId: String,
        type: String,
        travelDate: String,
    ): String? = buildList {
        var cursor: String? = null
        do {
            val page = backendApi.getRideRequests(box = SentBox, cursor = cursor).requireEnvelope()
            addAll(page.data)
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }.firstOrNull { request ->
        request.senderTrip.id == tripId &&
            request.targetTrip.id == candidateTripId &&
            request.type == type &&
            request.travelDate == travelDate &&
            request.status == PendingStatus
    }?.id

    private companion object {
        const val CoordinationNotFound = "COORDINATION_NOT_FOUND"
        const val RequestAlreadyExists = "REQUEST_ALREADY_EXISTS"
        const val SentBox = "sent"
        const val PendingStatus = "PENDING"
        const val RefreshIntervalMillis = 15_000L
    }
}

/** Convierte el día ISO del servidor al texto que usa el selector actual. */
private fun Int.toDayName(): String = when (this) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    7 -> "Domingo"
    else -> throw IllegalArgumentException("El servidor devolvió un día inválido.")
}
