package uvg.edu.rutau.core.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Muestra un push como aviso claro cuando la aplicación está abierta. */
@Composable
fun PushPopupHost(
    message: PushPopupMessage,
    onDismiss: () -> Unit,
    onOpen: (PushNavigationTarget) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(message.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ahora no")
            }
        },
        confirmButton = {
            val target = message.target
            TextButton(
                onClick = {
                    if (target != null) onOpen(target) else onDismiss()
                },
            ) {
                Text(if (target != null) "Ver actualización" else "Entendido")
            }
        },
    )
}
