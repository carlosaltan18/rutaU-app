package uvg.edu.rutau.core.preferences

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** Muestra un mensaje remoto una sola vez y guarda el descarte en DataStore. */
@Composable
fun PopupMessageHost(
    feedRepository: PopupMessageFeedRepository,
    localRepository: PopupMessageRepository,
    eventRepository: PopupMessageEventRepository,
    onAction: (PopupMessageAction, String?) -> Unit,
) {
    val messages by feedRepository.observeAvailableMessages().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val message = messages.firstOrNull()
    if (message != null) {
        LaunchedEffect(message.id) {
            eventRepository.record(message.id, PopupMessageEventType.SHOWN)
        }
        AlertDialog(
            onDismissRequest = {
                scope.launch {
                    localRepository.markAsSeen(message.id)
                    eventRepository.record(message.id, PopupMessageEventType.DISMISSED)
                }
            },
            title = { Text(message.title, style = MaterialTheme.typography.titleLarge) },
            text = { Text(message.body, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            localRepository.markAsSeen(message.id)
                            eventRepository.record(
                                message.id,
                                message.action.confirmEventType(),
                            )
                        }
                        onAction(message.action, message.actionValue)
                    },
                ) {
                    Text(message.action.buttonLabel)
                }
            },
        )
    }
}

/** Decide el evento correcto cuando la persona confirma el anuncio. */
private fun PopupMessageAction.confirmEventType(): PopupMessageEventType = when (this) {
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
