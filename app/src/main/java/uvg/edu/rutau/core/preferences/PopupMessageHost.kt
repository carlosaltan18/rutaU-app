package uvg.edu.rutau.core.preferences

import androidx.compose.material3.AlertDialog
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
) {
    val messages by feedRepository.observeAvailableMessages().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val message = messages.firstOrNull()
    if (message != null) {
        AlertDialog(
            onDismissRequest = { scope.launch { localRepository.markAsSeen(message.id) } },
            title = { Text(message.title) },
            text = { Text(message.body) },
            confirmButton = {
                TextButton(onClick = { scope.launch { localRepository.markAsSeen(message.id) } }) {
                    Text("Entendido")
                }
            },
        )
    }
}
