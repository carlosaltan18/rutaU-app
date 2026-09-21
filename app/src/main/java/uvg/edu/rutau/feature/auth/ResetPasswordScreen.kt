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
import uvg.edu.rutau.core.designsystem.component.RutaUPasswordField
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Muestra el formulario para cambiar la contraseña. */
@Composable
fun ResetPasswordScreen(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onUpdatePassword: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = "Restablecer contraseña",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "Volver",
                onNavigationClick = onBackToLogin,
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
                text = "Crea una nueva contraseña",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.Small))
            Text(
                text = "Ingresa una contraseña segura para volver a acceder a tu cuenta de RutaU.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.XXLarge))
            RutaUPasswordField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "Nueva contraseña",
                modifier = Modifier.fillMaxWidth().testTag("ResetPasswordInput"),
            )
            Spacer(Modifier.height(RutaUSpacing.Medium))
            RutaUPasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar contraseña",
                modifier = Modifier.fillMaxWidth().testTag("ResetConfirmPasswordInput"),
                supportingText = errorMessage,
                isError = errorMessage != null,
            )
            Spacer(Modifier.height(RutaUSpacing.Medium))
            Text(
                text = "Requisitos: al menos 8 caracteres, una letra mayúscula y un número.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            RutaULoadingButton(
                text = "Actualizar contraseña",
                onClick = onUpdatePassword,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth().testTag("ResetSubmitButton"),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordScreenPreview() {
    RutaUTheme {
        ResetPasswordScreen(
            newPassword = "",
            confirmPassword = "",
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onUpdatePassword = {},
            onBackToLogin = {},
        )
    }
}
