package uvg.edu.rutau.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.data.mock.MockSeed
import uvg.edu.rutau.core.designsystem.component.RutaUConfirmationDialog
import uvg.edu.rutau.core.designsystem.component.RutaUDestructiveButton
import uvg.edu.rutau.core.designsystem.component.RutaUInformationDialog
import uvg.edu.rutau.core.designsystem.component.RutaULoadingButton
import uvg.edu.rutau.core.designsystem.component.RutaUOutlinedButton
import uvg.edu.rutau.core.designsystem.component.RutaUPasswordField
import uvg.edu.rutau.core.designsystem.component.RutaUProfilePhotoPicker
import uvg.edu.rutau.core.designsystem.component.RutaUSectionTitle
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.core.designsystem.component.RutaUDropdownField
import uvg.edu.rutau.core.data.catalog.CatalogItem
import uvg.edu.rutau.core.preferences.ThemeMode
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.model.UserAccount
import uvg.edu.rutau.core.navigation.LegalDocumentType
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Guarda la información que se muestra en la cuenta. */
data class AccountUiState(
    val user: UserAccount,
    val fullName: String = user.fullName,
    val university: String = user.university,
    val universityId: String = user.universityId,
    val universityOptions: List<CatalogItem> = emptyList(),
    val campus: String = user.campus,
    val campusId: String = user.campusId,
    val campusOptions: List<CatalogItem> = emptyList(),
    val email: String = user.email,
    val currentEmailPassword: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val notificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSaving: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val legalDocument: LegalDocument? = null,
    val message: String? = null,
)

/** Indica el documento que se quiere consultar. */
enum class LegalDocument(
    val title: String,
    val content: String,
) {
    TERMS(
        title = "Términos y condiciones",
        content = "RutaU facilita el contacto entre estudiantes para coordinar trayectos. " +
            "Cada persona es responsable de la información que comparte y de los acuerdos de viaje que acepta.",
    ),
    PRIVACY(
        title = "Política de privacidad",
        content = "RutaU muestra solo los datos necesarios para la coordinación. La información de contacto " +
            "se comparte únicamente después de que una solicitud sea aceptada por ambas partes.",
    ),
    ABOUT(
        title = "Acerca de RutaU",
        content = "RutaU MVP v1.0 ayuda a estudiantes universitarios a encontrar compañeros compatibles " +
            "para sus trayectos habituales.",
    ),
}

/** Indica las acciones que puede elegir una persona en su cuenta. */
sealed interface AccountAction {
    data class FullNameChanged(val value: String) : AccountAction
    data class UniversityChanged(val value: String) : AccountAction
    data class CampusChanged(val value: String) : AccountAction
    data class EmailChanged(val value: String) : AccountAction
    data class CurrentEmailPasswordChanged(val value: String) : AccountAction
    data class CurrentPasswordChanged(val value: String) : AccountAction
    data class NewPasswordChanged(val value: String) : AccountAction
    data class ConfirmPasswordChanged(val value: String) : AccountAction
    data class PhotoChanged(val value: String?) : AccountAction
    data class NotificationsChanged(val enabled: Boolean) : AccountAction
    data class ThemeModeChanged(val value: ThemeMode) : AccountAction
    data object SaveProfile : AccountAction
    data object UpdateEmail : AccountAction
    data object UpdatePassword : AccountAction
    data object LogoutRequested : AccountAction
    data object LogoutConfirmed : AccountAction
    data object LogoutDismissed : AccountAction
    data class LegalDocumentRequested(val document: LegalDocument) : AccountAction
    data object LegalDocumentDismissed : AccountAction
    data object DeleteAccountRequested : AccountAction
    data object DeleteAccountConfirmed : AccountAction
    data object DeleteAccountDismissed : AccountAction
}

/** Muestra los datos y opciones de la cuenta. */
@Composable
fun AccountScreen(
    state: AccountUiState,
    onAction: (AccountAction) -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    modifier: Modifier = Modifier,
    onRequestNotificationPermission: () -> Unit = {},
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = "Cuenta",
                actions = {
                    val useLightMode = state.themeMode == ThemeMode.DARK
                    IconButton(
                        onClick = {
                            onAction(
                                AccountAction.ThemeModeChanged(
                                    if (useLightMode) ThemeMode.LIGHT else ThemeMode.DARK,
                                ),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = if (useLightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (useLightMode) "Activar modo claro" else "Activar modo oscuro",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = RutaUSpacing.ScreenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Small),
        ) {
            Text("Mi perfil", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Actualiza tus datos personales y de acceso por separado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            state.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            RutaUSectionTitle("Datos personales")
            AccountSectionCard {
                RutaUProfilePhotoPicker(
                    fullName = state.fullName,
                    photoUrl = state.user.photoUrl,
                    onPhotoSelected = { onAction(AccountAction.PhotoChanged(it)) },
                )
                RutaUTextField(
                    state.fullName,
                    { onAction(AccountAction.FullNameChanged(it)) },
                    "Nombre completo",
                    Modifier.fillMaxWidth().testTag("AccountFullNameInput"),
                )
                RutaUDropdownField(
                    selectedOption = state.university,
                    options = state.universityOptions.map(CatalogItem::name),
                    onOptionSelected = { name ->
                        state.universityOptions.firstOrNull { it.name == name }
                            ?.let { onAction(AccountAction.UniversityChanged(it.id)) }
                    },
                    label = "Universidad",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.universityOptions.isNotEmpty(),
                    supportingText = if (state.universityOptions.isEmpty()) "Cargando universidades disponibles..." else null,
                )
                RutaUDropdownField(
                    selectedOption = state.campus,
                    options = state.campusOptions.map(CatalogItem::name),
                    onOptionSelected = { name ->
                        state.campusOptions.firstOrNull { it.name == name }
                            ?.let { onAction(AccountAction.CampusChanged(it.id)) }
                    },
                    label = "Campus habitual",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.campusOptions.isNotEmpty(),
                    supportingText = if (state.campusOptions.isEmpty()) "Cargando campus disponibles..." else null,
                )
                RutaULoadingButton(
                    text = "Guardar datos personales",
                    onClick = { onAction(AccountAction.SaveProfile) },
                    isLoading = state.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("AccountSaveProfileButton"),
                )
            }

            RutaUSectionTitle("Credenciales")
            AccountSectionCard {
                Text("Correo electrónico", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Requiere tu contraseña actual para confirmar el cambio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                RutaUTextField(state.email, { onAction(AccountAction.EmailChanged(it)) }, "Nuevo correo", Modifier.fillMaxWidth())
                RutaUPasswordField(
                    state.currentEmailPassword,
                    { onAction(AccountAction.CurrentEmailPasswordChanged(it)) },
                    "Contraseña actual",
                    Modifier.fillMaxWidth(),
                )
                RutaUOutlinedButton(
                    text = "Actualizar correo",
                    onClick = { onAction(AccountAction.UpdateEmail) },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
                Text("Contraseña", style = MaterialTheme.typography.titleSmall)
                RutaUPasswordField(state.currentPassword, { onAction(AccountAction.CurrentPasswordChanged(it)) }, "Contraseña actual", Modifier.fillMaxWidth())
                RutaUPasswordField(state.newPassword, { onAction(AccountAction.NewPasswordChanged(it)) }, "Nueva contraseña", Modifier.fillMaxWidth())
                RutaUPasswordField(state.confirmPassword, { onAction(AccountAction.ConfirmPasswordChanged(it)) }, "Confirmar nueva contraseña", Modifier.fillMaxWidth())
                Text(
                    text = "Usa al menos 8 caracteres, una mayúscula y un número.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                RutaUOutlinedButton(
                    text = "Actualizar contraseña",
                    onClick = { onAction(AccountAction.UpdatePassword) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            RutaUSectionTitle("Preferencias")
            AccountSectionCard {
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
                        onCheckedChange = { enabled ->
                            if (enabled) onRequestNotificationPermission()
                            onAction(AccountAction.NotificationsChanged(enabled))
                        },
                    )
                }
            }

            RutaUSectionTitle("Información y legal")
            AccountSectionCard {
                LegalDocumentButton(
                    text = LegalDocument.TERMS.title,
                    onClick = { onOpenLegalDocument(LegalDocumentType.TERMS) },
                )
                LegalDocumentButton(
                    text = LegalDocument.PRIVACY.title,
                    onClick = { onOpenLegalDocument(LegalDocumentType.PRIVACY) },
                )
                LegalDocumentButton(
                    text = LegalDocument.ABOUT.title,
                    onClick = { onAction(AccountAction.LegalDocumentRequested(LegalDocument.ABOUT)) },
                )
            }

            RutaUSectionTitle("Sesión")
            AccountSectionCard {
                RutaUOutlinedButton(
                    text = "Cerrar sesión",
                    onClick = { onAction(AccountAction.LogoutRequested) },
                    modifier = Modifier.fillMaxWidth().testTag("AccountLogoutButton"),
                )
                RutaUDestructiveButton(
                    text = "Eliminar mi cuenta",
                    onClick = { onAction(AccountAction.DeleteAccountRequested) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(RutaUSpacing.Medium))
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

    if (state.showLogoutConfirmation) {
        RutaUConfirmationDialog(
            title = "¿Cerrar sesión?",
            message = "Tendrás que ingresar tus credenciales para volver a acceder a RutaU.",
            confirmLabel = "Cerrar sesión",
            onConfirm = { onAction(AccountAction.LogoutConfirmed) },
            onDismiss = { onAction(AccountAction.LogoutDismissed) },
            modifier = Modifier.testTag("AccountLogoutConfirmation"),
        )
    }

    state.legalDocument?.let { document ->
        RutaUInformationDialog(
            title = document.title,
            message = document.content,
            onDismiss = { onAction(AccountAction.LegalDocumentDismissed) },
        )
    }
}

/** Agrupa controles que se guardan o consultan dentro de una misma sección. */
@Composable
private fun AccountSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(RutaUSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Medium),
            content = content,
        )
    }
}

@Composable
private fun LegalDocumentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("›", style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun LegalDocumentButtonPreview() {
    RutaUTheme {
        LegalDocumentButton(
            text = "Términos y condiciones",
            onClick = {},
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
            onOpenLegalDocument = {},
        )
    }
}
