package uvg.edu.rutau.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import uvg.edu.rutau.MainActivity
import uvg.edu.rutau.R
import uvg.edu.rutau.app.RutaUAppDependencies

/** Identifica los avisos que puede mostrar la aplicación. */
enum class RutaUPushType {
    REQUEST_RECEIVED,
    REQUEST_ACCEPTED,
    REQUEST_REJECTED,
    REQUEST_CANCELLED,
    REQUEST_EXPIRED,
    COORDINATION_CANCELLED,
}

/** Recibe los avisos de Firebase y los muestra sin exponer datos privados. */
class RutaUFirebaseMessagingService : FirebaseMessagingService() {
    @Deprecated("Firebase mantiene este método para renovar el token.")
    override fun onNewToken(token: String) {
        Log.i(LogTag, "Firebase renovó el token de avisos.")
        RutaUAppDependencies.registerFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data[PushTypeKey]
            ?.let { value -> RutaUPushType.entries.firstOrNull { it.name == value } }
        val destination = message.data[DestinationKey].orEmpty()
        val resourceId = message.data[ResourceIdKey].orEmpty()
        val target = type
            ?.takeIf { destination.isNotBlank() && resourceId.isNotBlank() }
            ?.let { PushNavigationTarget(it, destination, resourceId) }
        val title = message.notification?.title ?: type?.title() ?: "Actualización de RutaU"
        val body = message.notification?.body ?: type?.message() ?: "Tienes una nueva actualización."
        val notificationId = message.data[NotificationIdKey].orEmpty()

        Log.i(LogTag, "Llegó un aviso de tipo ${type?.name ?: "sin tipo"}.")
        PushPopupStore.publish(PushPopupMessage(title, body, target))
        RutaUAppDependencies.markNotificationHandled(notificationId)
        target?.let {
            RutaUNotificationManager(applicationContext).show(it, title, body, notificationId)
        }
    }

    private companion object {
        const val PushTypeKey = "type"
        const val DestinationKey = "destination"
        const val ResourceIdKey = "resourceId"
        const val NotificationIdKey = "notificationId"
        const val LogTag = "RutaUPush"
    }
}

/** Crea canales y avisos simples para las actividades de un viaje. */
class RutaUNotificationManager(
    private val context: Context,
) {
    fun show(
        target: PushNavigationTarget,
        title: String = "Actualización de RutaU",
        body: String = target.type.message(),
        notificationId: String = "",
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putPushNavigationTarget(target)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            target.type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, RideChannelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val id = notificationId.takeIf(String::isNotBlank)?.hashCode() ?: target.type.ordinal
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Crea el canal antes de que Firebase muestre un aviso en segundo plano. */
    fun ensureChannel() {
        val channel = NotificationChannel(
            RideChannelId,
            "Actividad de viajes",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Avisos sobre solicitudes y viajes coordinados." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val RideChannelId = "rutau_updates"
    }
}

/** Da un título breve a cada aviso de Firebase. */
internal fun RutaUPushType.title(): String = when (this) {
    RutaUPushType.REQUEST_RECEIVED -> "Nueva solicitud de viaje"
    RutaUPushType.REQUEST_ACCEPTED -> "Solicitud aceptada"
    RutaUPushType.REQUEST_REJECTED -> "Solicitud rechazada"
    RutaUPushType.REQUEST_CANCELLED -> "Solicitud cancelada"
    RutaUPushType.REQUEST_EXPIRED -> "Solicitud expirada"
    RutaUPushType.COORDINATION_CANCELLED -> "Viaje cancelado"
}

/** Convierte un tipo técnico en un mensaje breve para la notificación. */
internal fun RutaUPushType.message(): String = when (this) {
    RutaUPushType.REQUEST_RECEIVED -> "Tienes una nueva solicitud de viaje."
    RutaUPushType.REQUEST_ACCEPTED -> "Tu solicitud fue aceptada."
    RutaUPushType.REQUEST_REJECTED -> "Tu solicitud fue rechazada."
    RutaUPushType.REQUEST_CANCELLED -> "Una solicitud fue cancelada."
    RutaUPushType.REQUEST_EXPIRED -> "Una solicitud venció antes de ser aceptada."
    RutaUPushType.COORDINATION_CANCELLED -> "Un viaje coordinado fue cancelado."
}
