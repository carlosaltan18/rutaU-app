package uvg.edu.rutau.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Muestra información breve sin permitir cambios. */
@Composable
fun RutaUInformationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Entendido",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            RutaUPrimaryButton(
                text = dismissLabel,
                onClick = onDismiss,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun RutaUInformationDialogPreview() {
    RutaUTheme {
        RutaUInformationDialog(
            title = "Acerca de RutaU",
            message = "RutaU facilita la coordinación de trayectos universitarios.",
            onDismiss = {},
        )
    }
}
