package uvg.edu.rutau.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Diálogo para confirmar acciones que requieren una decisión explícita del estudiante. */
@Composable
fun RutaUConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    dismissLabel: String = "Cancelar",
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (isDestructive) {
                RutaUDestructiveButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                )
            } else {
                RutaUPrimaryButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                )
            }
        },
        dismissButton = {
            RutaUOutlinedButton(
                text = dismissLabel,
                onClick = onDismiss,
            )
        },
    )
}
