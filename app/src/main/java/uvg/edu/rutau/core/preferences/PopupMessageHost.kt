package uvg.edu.rutau.core.preferences

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Muestra un anuncio recibido sin conocer cómo se guardan sus datos. */
@Composable
fun PopupMessageHost(
    message: AppPopupMessage?,
    onShown: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (message != null) {
        LaunchedEffect(message.id) {
            onShown(message.id)
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(message.title, style = MaterialTheme.typography.titleLarge) },
            text = { Text(message.body, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                ) {
                    Text(message.action.buttonLabel)
                }
            },
        )
    }
}

/** Decide el evento correcto cuando la persona confirma el anuncio. */
internal fun PopupMessageAction.confirmEventType(): PopupMessageEventType = when (this) {
    PopupMessageAction.NONE -> PopupMessageEventType.DISMISSED
    PopupMessageAction.OPEN_PROFILE,
    PopupMessageAction.OPEN_TRIPS,
    PopupMessageAction.OPEN_REQUESTS,
    -> PopupMessageEventType.OPENED
}

/** Da un texto breve al botón según la acción elegida para el anuncio. */
private val PopupMessageAction.buttonLabel: String
    get() = when (this) {
        PopupMessageAction.NONE -> "Entendido"
        PopupMessageAction.OPEN_PROFILE -> "Ver perfil"
        PopupMessageAction.OPEN_TRIPS -> "Ver trayectos"
        PopupMessageAction.OPEN_REQUESTS -> "Ver solicitudes"
    }
