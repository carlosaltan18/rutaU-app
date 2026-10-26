package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestType
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.BackendApiException
import uvg.edu.rutau.core.network.CreateRideRequestRequestDto
import uvg.edu.rutau.core.network.IdempotencyKeyStore
import uvg.edu.rutau.core.network.nextTravelDate
import uvg.edu.rutau.core.network.requireData
import uvg.edu.rutau.core.network.toCoordination

/** Conecta los viajes coordinados con las llamadas reales del servidor. */
class RemoteCoordinationRepository(
    private val backendApi: BackendApi,
) : CoordinationRepository {
    private val idempotencyKeys = IdempotencyKeyStore()
    override fun observeCoordination(requestId: String): Flow<Coordination?> = flow {
        val coordination = try {
            backendApi.getCoordinationForRequest(requestId).requireData().toCoordination()
        } catch (error: BackendApiException) {
            if (error.code == CoordinationNotFound) null else throw error
        }
        emit(coordination)
    }

    override fun observeCoordinationById(coordinationId: String): Flow<Coordination?> = flow {
        emit(backendApi.getCoordination(coordinationId).requireData().toCoordination())
    }

    override suspend fun cancelRide(coordinationId: String) {
        backendApi.cancelCoordination(coordinationId).requireData()
    }

    override suspend fun cancelParticipation(coordinationId: String) {
        backendApi.cancelCoordination(coordinationId).requireData()
    }

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
        val operation = "request:$tripId:$candidateTripId:$type:$contributionCents:${message.orEmpty()}"
        val requestId = backendApi.createRideRequest(
            idempotencyKey = idempotencyKeys.keyFor(operation),
            body = CreateRideRequestRequestDto(
                senderTripId = tripId,
                targetTripId = candidateTripId,
                type = type.name,
                travelDate = nextTravelDate(source.dayOfWeek.toDayName()),
                offeredContributionMinor = contributionCents,
                message = message?.trim()?.takeIf(String::isNotEmpty),
            ),
        ).requireData().id
        idempotencyKeys.complete(operation)
        return requestId
    }

    private companion object {
        const val CoordinationNotFound = "COORDINATION_NOT_FOUND"
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
