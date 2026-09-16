package uvg.edu.rutau.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.designsystem.component.RutaULoadingButton
import uvg.edu.rutau.core.designsystem.component.RutaUPasswordField
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Pantalla pura de inicio de sesión; la Route administra el estado y la navegación. */
@Composable
fun LoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    RutaUScreenContainer(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = RutaUSpacing.ScreenHorizontal)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "RutaU",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            Text(
                text = "Bienvenido a RutaU",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.Small))
            Text(
                text = "Encuentra estudiantes con trayectos compatibles hacia tu campus.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RutaUSpacing.XXLarge))
            RutaUTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
            )
            Spacer(Modifier.height(RutaUSpacing.Medium))
            RutaUPasswordField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth(),
                supportingText = errorMessage,
                isError = errorMessage != null,
            )
            Spacer(Modifier.height(RutaUSpacing.Small))
            androidx.compose.material3.TextButton(onClick = onForgotPassword) {
                Text("¿Olvidaste tu contraseña?")
            }
            Spacer(Modifier.height(RutaUSpacing.Large))
            RutaULoadingButton(
                text = "Iniciar sesión",
                onClick = onLogin,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            Text(
                text = "¿No tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium,
            )
            RutaUPrimaryButton(
                text = "Crear cuenta",
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            Text(
                text = "Al continuar, aceptas nuestros Términos y condiciones y Política de privacidad.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    RutaUTheme {
        LoginScreen(
            email = "mateo@ejemplo.com",
            password = "RutaU123",
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
            onForgotPassword = {},
            onSignUp = {},
        )
    }
}
