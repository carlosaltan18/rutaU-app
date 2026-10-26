package uvg.edu.rutau.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import uvg.edu.rutau.core.model.RideRequest
import uvg.edu.rutau.core.model.RideRequestDetails
import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.BackendRideRequestDto
import uvg.edu.rutau.core.network.requireData
import uvg.edu.rutau.core.network.requireEnvelope
import uvg.edu.rutau.core.network.toRideRequestDetails

/** Consulta y modifica solicitudes usando los datos reales del servidor. */
class RemoteRideRequestRepository(
    private val backendApi: BackendApi,
) : RideRequestRepository {
    private val details = MutableStateFlow<List<RideRequestDetails>>(emptyList())

    override fun observeRequests(): Flow<List<RideRequest>> = flow {
        refreshRequests()
        emitAll(details.map { current -> current.map { it.request } })
    }

    override fun observeRequest(requestId: String): Flow<RideRequest?> = flow {
        val detail = loadRequest(requestId)
        emit(detail.request)
    }

    override fun observeRequestDetails(): Flow<List<RideRequestDetails>> = flow {
        refreshRequests()
        emitAll(details)
    }

    override fun observeRequestDetail(requestId: String): Flow<RideRequestDetails?> = flow {
        emit(loadRequest(requestId))
    }

    override suspend fun accept(requestId: String) {
        replace(backendApi.acceptRideRequest(requestId).requireData().toRideRequestDetails())
    }

    override suspend fun reject(requestId: String) {
        replace(backendApi.rejectRideRequest(requestId).requireData().toRideRequestDetails())
    }

    override suspend fun cancel(requestId: String) {
        replace(backendApi.cancelRideRequest(requestId).requireData().toRideRequestDetails())
    }

    override suspend fun expire(requestId: String) {
        throw UnsupportedOperationException("El servidor vence las solicitudes automáticamente cuando pasa la hora del viaje.")
    }

    private suspend fun loadRequest(requestId: String): RideRequestDetails {
        val loaded = backendApi.getRideRequest(requestId).requireData().toRideRequestDetails()
        replace(loaded)
        return loaded
    }

    private suspend fun refreshRequests() {
        val sent = loadBox(SentBox)
        val received = loadBox(ReceivedBox)
        details.value = (sent + received)
            .map { it.toRideRequestDetails() }
            .distinctBy { it.request.id }
    }

    private suspend fun loadBox(box: String): List<BackendRideRequestDto> = buildList {
        var cursor: String? = null
        do {
            val page = backendApi.getRideRequests(box = box, cursor = cursor).requireEnvelope()
            addAll(page.data)
            cursor = page.meta.nextCursor
        } while (cursor != null)
    }

    private fun replace(updated: RideRequestDetails) {
        details.value = details.value.filterNot { it.request.id == updated.request.id } + updated
    }

    private companion object {
        const val SentBox = "sent"
        const val ReceivedBox = "received"
    }
}
