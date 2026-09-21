package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.RideRequest

/** Source of truth for the state machine of ride requests and invitations. */
interface RideRequestRepository {
    fun observeRequests(): Flow<List<RideRequest>>
    fun observeRequest(requestId: String): Flow<RideRequest?>

    suspend fun accept(requestId: String)
    suspend fun reject(requestId: String)
    suspend fun cancel(requestId: String)
    suspend fun expire(requestId: String)
}
