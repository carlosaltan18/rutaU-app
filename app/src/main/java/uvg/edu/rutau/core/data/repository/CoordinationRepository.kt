package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.Coordination
import uvg.edu.rutau.core.model.RequestType

interface CoordinationRepository {
    fun observeCoordination(requestId: String): Flow<Coordination?>

    suspend fun cancelRide(coordinationId: String)
    suspend fun cancelParticipation(coordinationId: String)

    /**
     * Transitional creation entry point used by the Trips module until it depends directly on
     * [RideRequestRepository]. It always creates a pending request and never reserves a seat.
     */
    suspend fun createPendingCoordination(
        tripId: String,
        candidateTripId: String,
        type: RequestType,
        message: String?,
        contributionCents: Long,
    ): String
}
