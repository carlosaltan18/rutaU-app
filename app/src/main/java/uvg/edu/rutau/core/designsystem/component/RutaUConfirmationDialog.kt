package uvg.edu.rutau.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Diálogo para confirmar acciones que requieren una decisión explícita del estudiante. */
@Composable
fun RutaUConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
                    modifier = Modifier.testTag("ConfirmationDialogConfirmButton"),
                )
            } else {
                RutaUPrimaryButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.testTag("ConfirmationDialogConfirmButton"),
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
