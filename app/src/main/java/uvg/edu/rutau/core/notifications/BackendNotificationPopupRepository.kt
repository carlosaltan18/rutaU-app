package uvg.edu.rutau.core.notifications

import uvg.edu.rutau.core.network.BackendApi
import uvg.edu.rutau.core.network.requireData
import uvg.edu.rutau.core.network.requireEnvelope

/** Consulta los avisos internos cuando Firebase no pudo entregarlos a tiempo. */
interface NotificationPopupRepository {
    suspend fun refresh()
    suspend fun markHandled(notificationId: String)
}

/** Convierte avisos del servidor en mensajes breves para la interfaz. */
class BackendNotificationPopupRepository(
    private val backendApi: BackendApi,
) : NotificationPopupRepository {
    override suspend fun refresh() {
        val notifications = backendApi.getNotifications(limit = PageSize).requireEnvelope().data
        notifications.asReversed()
            .filter { it.readAt == null }
            .forEach { notification ->
                val type = RutaUPushType.entries.firstOrNull { it.name == notification.type }
                val target = type
                    ?.let { pushType ->
                        notification.resourceId?.takeIf(String::isNotBlank)?.let { resourceId ->
                            PushNavigationTarget(
                                type = pushType,
                                destination = if (pushType == RutaUPushType.COORDINATION_CANCELLED) {
                                    "COORDINATION_DETAIL"
                                } else {
                                    "RIDE_REQUEST_DETAIL"
                                },
                                resourceId = resourceId,
                            )
                        }
                    }
                PushPopupStore.publish(
                    PushPopupMessage(
                        title = type?.title() ?: "Actualización de RutaU",
                        body = type?.message() ?: "Tienes una nueva actualización.",
                        target = target,
                    ),
                )
                markHandled(notification.id)
            }
    }

    override suspend fun markHandled(notificationId: String) {
        if (notificationId.isBlank()) return
        backendApi.markNotificationRead(notificationId).requireData()
    }

    private companion object {
        const val PageSize = 30
    }
}
