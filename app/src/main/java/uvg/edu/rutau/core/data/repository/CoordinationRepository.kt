package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestType

/** Indica las acciones disponibles para viajes ya aceptados. */
interface CoordinationRepository {
    fun observeCoordination(requestId: String): Flow<Coordination?>
    fun observeCoordinationById(coordinationId: String): Flow<Coordination?>

    suspend fun cancelRide(coordinationId: String): Coordination
    suspend fun cancelParticipation(coordinationId: String): Coordination
    suspend fun updateMeetingPoint(coordinationId: String, meetingPoint: String): Coordination
    suspend fun confirmMeetingPoint(coordinationId: String): Coordination
    suspend fun startLiveLocation(coordinationId: String): Coordination
    suspend fun endLiveLocation(coordinationId: String): Coordination

    /** Crea una solicitud pendiente sin ocupar una plaza. */
    suspend fun createPendingCoordination(
        tripId: String,
        candidateTripId: String,
        type: RequestType,
        message: String?,
        contributionCents: Long,
    ): String
}
