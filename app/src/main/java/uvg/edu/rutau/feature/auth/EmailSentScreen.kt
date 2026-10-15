package uvg.edu.rutau.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.designsystem.component.RutaUInfoCard
import uvg.edu.rutau.core.designsystem.component.RutaUPrimaryButton
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Muestra la confirmación después de pedir recuperar el acceso. */
@Composable
fun EmailSentScreen(
    onResend: () -> Unit,
    onOpenResetLink: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RutaUScreenContainer(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = RutaUSpacing.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            Text(
                text = "Revisa tu correo",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(RutaUSpacing.Small))
            Text(
                text = "Si existe una cuenta asociada, recibirás instrucciones para restablecer tu contraseña.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            RutaUInfoCard(
                title = "Validez de 15 minutos",
                message = "Por seguridad, el enlace temporal expirará después de ese tiempo.",
            )
            Spacer(Modifier.height(RutaUSpacing.XLarge))
            RutaUPrimaryButton(
                text = "Enviar nuevamente",
                onClick = onResend,
                modifier = Modifier.fillMaxWidth().testTag("RecoveryResendButton"),
            )
            Spacer(Modifier.height(RutaUSpacing.Medium))
            Text(
                text = "¿Abriste el enlace de tu correo?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            RutaUPrimaryButton(
                text = "Abrir enlace de demostración",
                onClick = onOpenResetLink,
                modifier = Modifier.fillMaxWidth().testTag("RecoveryOpenDemoLinkButton"),
            )
            Spacer(Modifier.height(RutaUSpacing.Medium))
            androidx.compose.material3.TextButton(onClick = onBackToLogin) {
                Text("Volver a iniciar sesión")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailSentScreenPreview() {
    RutaUTheme {
        EmailSentScreen(onResend = {}, onOpenResetLink = {}, onBackToLogin = {})
    }
}
