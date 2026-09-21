package uvg.edu.rutau.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.designsystem.component.RutaULoadingButton
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Muestra el formulario para recuperar el acceso. */
@Composable
fun RecoverAccessScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendInstructions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = "Recuperar acceso",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "Volver",
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = RutaUSpacing.ScreenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.Small))
            Text(
                text = "Ingresa el correo asociado a tu cuenta. Te enviaremos instrucciones para restablecer tu contraseña.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.XXLarge))
            RutaUTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                modifier = Modifier.fillMaxWidth().testTag("RecoveryEmailInput"),
                supportingText = errorMessage,
                isError = errorMessage != null,
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            RutaULoadingButton(
                text = "Enviar instrucciones",
                onClick = onSendInstructions,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth().testTag("RecoverySubmitButton"),
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            Text(
                text = "Por seguridad y privacidad, no confirmaremos si el correo ingresado está asociado con una cuenta.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoverAccessScreenPreview() {
    RutaUTheme {
        RecoverAccessScreen(
            email = "mateo@ejemplo.com",
            onEmailChange = {},
            onSendInstructions = {},
            onBack = {},
        )
    }
}
