package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.CancellationReason
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.MeetingPointInput
import uvg.edu.rutau.core.model.RequestType

/** Indica las acciones disponibles para viajes ya aceptados. */
interface CoordinationRepository {
    fun observeCoordinations(): Flow<List<Coordination>>
    fun observeCoordination(requestId: String): Flow<Coordination?>
    fun observeCoordinationById(coordinationId: String): Flow<Coordination?>

    suspend fun cancelRide(coordinationId: String, reason: CancellationReason): Coordination
    suspend fun cancelParticipation(coordinationId: String, reason: CancellationReason): Coordination
    suspend fun markArrival(coordinationId: String): Coordination
    suspend fun confirmContribution(coordinationId: String): Coordination
    suspend fun updateMeetingPoint(coordinationId: String, meetingPoint: MeetingPointInput): Coordination
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
