package uvg.edu.rutau.core.notifications

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Describe el recurso que debe consultar la aplicación después de un aviso. */
data class PushNavigationTarget(
    val type: RutaUPushType,
    val destination: String,
    val resourceId: String,
)

/** Mantiene una navegación pendiente mientras se abre la aplicación. */
object PushNavigationStore {
    private val mutableTargets = MutableSharedFlow<PushNavigationTarget>(replay = 1, extraBufferCapacity = 1)
    val targets: SharedFlow<PushNavigationTarget> = mutableTargets.asSharedFlow()

    fun publish(intent: Intent?) {
        intent?.toPushNavigationTarget()?.let(mutableTargets::tryEmit)
    }
}

/** Lee solo los identificadores mínimos que el servidor debe enviar en el push. */
fun Intent.toPushNavigationTarget(): PushNavigationTarget? {
    val type = getStringExtra(PushTypeKey)
        ?.let { value -> RutaUPushType.entries.firstOrNull { it.name == value } }
        ?: return null
    val destination = getStringExtra(DestinationKey).orEmpty()
    val resourceId = getStringExtra(ResourceIdKey).orEmpty()
    if (destination.isBlank() || resourceId.isBlank()) return null
    return PushNavigationTarget(type, destination, resourceId)
}

/** Agrega el destino de una notificación al intent que abre RutaU. */
fun Intent.putPushNavigationTarget(target: PushNavigationTarget): Intent = apply {
    putExtra(PushTypeKey, target.type.name)
    putExtra(DestinationKey, target.destination)
    putExtra(ResourceIdKey, target.resourceId)
}

private const val PushTypeKey = "type"
private const val DestinationKey = "destination"
private const val ResourceIdKey = "resourceId"
