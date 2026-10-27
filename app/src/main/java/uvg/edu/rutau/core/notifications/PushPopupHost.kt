package uvg.edu.rutau.core.notifications

import androidx.compose.material3.AlertDialog
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
        title = { Text(message.title) },
        text = { Text(message.body) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        confirmButton = {
            val target = message.target
            TextButton(
                onClick = {
                    if (target != null) onOpen(target) else onDismiss()
                },
            ) {
                Text(if (target != null) "Ver detalle" else "Entendido")
            }
        },
    )
}
