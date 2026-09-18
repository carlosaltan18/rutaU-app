package uvg.edu.rutau.core.data.repository

import uvg.edu.rutau.core.model.RequestType

interface CoordinationRepository {
    suspend fun createPendingCoordination(
        tripId: String,
        candidateTripId: String,
        type: RequestType,
        message: String?,
        contributionCents: Long,
    ): String
}
