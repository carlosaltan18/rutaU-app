package uvg.edu.rutau.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.data.mock.MockSeed
import uvg.edu.rutau.core.designsystem.component.RutaUAvatar
import uvg.edu.rutau.core.designsystem.component.RutaUConfirmationDialog
import uvg.edu.rutau.core.designsystem.component.RutaUDestructiveButton
import uvg.edu.rutau.core.designsystem.component.RutaULoadingButton
import uvg.edu.rutau.core.designsystem.component.RutaUOutlinedButton
import uvg.edu.rutau.core.designsystem.component.RutaUPasswordField
import uvg.edu.rutau.core.designsystem.component.RutaUSectionTitle
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.model.UserAccount
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

data class AccountUiState(
    val user: UserAccount,
    val fullName: String = user.fullName,
    val university: String = user.university,
    val campus: String = user.campus,
    val email: String = user.email,
    val currentEmailPassword: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val message: String? = null,
)

sealed interface AccountAction {
    data class FullNameChanged(val value: String) : AccountAction
    data class UniversityChanged(val value: String) : AccountAction
    data class CampusChanged(val value: String) : AccountAction
    data class EmailChanged(val value: String) : AccountAction
    data class CurrentEmailPasswordChanged(val value: String) : AccountAction
    data class CurrentPasswordChanged(val value: String) : AccountAction
    data class NewPasswordChanged(val value: String) : AccountAction
    data class ConfirmPasswordChanged(val value: String) : AccountAction
    data class NotificationsChanged(val enabled: Boolean) : AccountAction
    data object SaveProfile : AccountAction
    data object UpdateEmail : AccountAction
    data object UpdatePassword : AccountAction
    data object Logout : AccountAction
    data object DeleteAccountRequested : AccountAction
    data object DeleteAccountConfirmed : AccountAction
    data object DeleteAccountDismissed : AccountAction
}

/** Account and settings screen driven entirely by immutable state and events. */
@Composable
fun AccountScreen(
    state: AccountUiState,
    onAction: (AccountAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = { RutaUTopAppBar(title = "Cuenta") },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = RutaUSpacing.ScreenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Medium),
        ) {
            Spacer(Modifier.height(RutaUSpacing.Small))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RutaUAvatar(
                    fullName = state.user.fullName,
                    photoUrl = state.user.photoUrl,
                    size = RutaUSpacing.Huge,
                )
                Spacer(Modifier.height(RutaUSpacing.Small))
                Text(state.user.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${state.user.university} · ${state.user.campus}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            RutaUSectionTitle("Datos personales")
            RutaUTextField(state.fullName, { onAction(AccountAction.FullNameChanged(it)) }, "Nombre completo", Modifier.fillMaxWidth())
            RutaUTextField(state.university, { onAction(AccountAction.UniversityChanged(it)) }, "Universidad", Modifier.fillMaxWidth())
            RutaUTextField(state.campus, { onAction(AccountAction.CampusChanged(it)) }, "Campus habitual", Modifier.fillMaxWidth())
            RutaULoadingButton(
                text = "Guardar datos",
                onClick = { onAction(AccountAction.SaveProfile) },
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            RutaUSectionTitle("Correo electrónico")
            RutaUTextField(state.email, { onAction(AccountAction.EmailChanged(it)) }, "Nuevo correo", Modifier.fillMaxWidth())
            RutaUPasswordField(
                state.currentEmailPassword,
                { onAction(AccountAction.CurrentEmailPasswordChanged(it)) },
                "Contraseña actual para confirmar",
                Modifier.fillMaxWidth(),
            )
            RutaUOutlinedButton(
                text = "Actualizar correo",
                onClick = { onAction(AccountAction.UpdateEmail) },
                modifier = Modifier.fillMaxWidth(),
            )

            RutaUSectionTitle("Seguridad y contraseña")
            RutaUPasswordField(state.currentPassword, { onAction(AccountAction.CurrentPasswordChanged(it)) }, "Contraseña actual", Modifier.fillMaxWidth())
            RutaUPasswordField(state.newPassword, { onAction(AccountAction.NewPasswordChanged(it)) }, "Nueva contraseña", Modifier.fillMaxWidth())
            RutaUPasswordField(state.confirmPassword, { onAction(AccountAction.ConfirmPasswordChanged(it)) }, "Confirmar nueva contraseña", Modifier.fillMaxWidth())
            RutaUOutlinedButton(
                text = "Actualizar contraseña",
                onClick = { onAction(AccountAction.UpdatePassword) },
                modifier = Modifier.fillMaxWidth(),
            )

            RutaUSectionTitle("Preferencias")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notificaciones de solicitudes", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Avisos cuando una persona solicite o responda tu viaje.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { onAction(AccountAction.NotificationsChanged(it)) },
                )
            }

            RutaUSectionTitle("Información y legal")
            Text("Términos y condiciones", style = MaterialTheme.typography.bodyLarge)
            Text("Política de privacidad", style = MaterialTheme.typography.bodyLarge)
            Text("Acerca de RutaU", style = MaterialTheme.typography.bodyLarge)

            state.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            RutaUOutlinedButton(
                text = "Cerrar sesión",
                onClick = { onAction(AccountAction.Logout) },
                modifier = Modifier.fillMaxWidth(),
            )
            RutaUDestructiveButton(
                text = "Eliminar mi cuenta",
                onClick = { onAction(AccountAction.DeleteAccountRequested) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(RutaUSpacing.Large))
        }
    }

    if (state.showDeleteConfirmation) {
        RutaUConfirmationDialog(
            title = "¿Eliminar tu cuenta?",
            message = "Esta acción eliminará tus datos locales y no se puede deshacer.",
            confirmLabel = "Eliminar cuenta",
            onConfirm = { onAction(AccountAction.DeleteAccountConfirmed) },
            onDismiss = { onAction(AccountAction.DeleteAccountDismissed) },
            isDestructive = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenPreview() {
    RutaUTheme {
        AccountScreen(
            state = AccountUiState(user = MockSeed.currentUser),
            onAction = {},
        )
    }
}
