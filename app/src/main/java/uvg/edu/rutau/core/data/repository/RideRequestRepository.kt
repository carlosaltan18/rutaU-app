package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.RideRequestDetails

/** Indica las acciones disponibles para solicitudes e invitaciones. */
interface RideRequestRepository {
    fun observeRequests(): Flow<List<RideRequest>>
    fun observeRequest(requestId: String): Flow<RideRequest?>
    fun observeRequestDetails(): Flow<List<RideRequestDetails>>
    fun observeRequestDetail(requestId: String): Flow<RideRequestDetails?>

    /** Actualiza la lista para mostrar de inmediato los cambios de un viaje. */
    suspend fun refresh()

    suspend fun accept(requestId: String)
    suspend fun reject(requestId: String)
    suspend fun cancel(requestId: String)
    suspend fun expire(requestId: String)
}
