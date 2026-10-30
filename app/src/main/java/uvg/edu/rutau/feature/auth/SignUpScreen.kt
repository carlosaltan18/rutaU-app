package uvg.edu.rutau.feature.auth

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uvg.edu.rutau.core.data.catalog.CatalogItem
import uvg.edu.rutau.core.designsystem.component.RutaULoadingButton
import uvg.edu.rutau.core.designsystem.component.RutaUDropdownField
import uvg.edu.rutau.core.designsystem.component.RutaUPasswordField
import uvg.edu.rutau.core.designsystem.component.RutaUProfilePhotoPicker
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Muestra el formulario para crear una cuenta. */
@Composable
fun SignUpScreen(
    fullName: String,
    university: String,
    campus: String,
    email: String,
    password: String,
    confirmPassword: String,
    photoUrl: String?,
    universityOptions: List<CatalogItem>,
    campusOptions: List<CatalogItem>,
    termsAccepted: Boolean,
    onFullNameChange: (String) -> Unit,
    onUniversityChange: (String) -> Unit,
    onCampusChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPhotoChange: (String?) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onCreateAccountWithGoogle: () -> Unit,
    onBack: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isFormValid: Boolean = false,
    isGoogleFormValid: Boolean = false,
    fullNameError: String? = null,
    universityError: String? = null,
    campusError: String? = null,
    emailError: String? = null,
    passwordError: String? = null,
    confirmPasswordError: String? = null,
    termsError: String? = null,
    catalogError: String? = null,
) {
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = "Crear cuenta",
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
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Small),
        ) {
            Text(
                text = "Crea tu cuenta en RutaU",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Conéctate con la comunidad universitaria para compartir trayectos diarios.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            RutaUTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = "Nombre completo",
                modifier = Modifier.fillMaxWidth().testTag("SignUpFullNameInput"),
                supportingText = fullNameError,
                isError = fullNameError != null,
            )
            RutaUProfilePhotoPicker(
                fullName = fullName,
                photoUrl = photoUrl,
                onPhotoSelected = onPhotoChange,
                modifier = Modifier.fillMaxWidth().testTag("SignUpPhotoPicker"),
                label = "Foto (opcional)",
                avatarSize = 56.dp,
            )
            RutaUDropdownField(
                selectedOption = universityOptions.firstOrNull { it.id == university }?.name,
                options = universityOptions.map(CatalogItem::name),
                onOptionSelected = { name ->
                    universityOptions.firstOrNull { it.name == name }?.let { onUniversityChange(it.id) }
                },
                label = "Universidad",
                modifier = Modifier.fillMaxWidth().testTag("SignUpUniversitySelector"),
                supportingText = universityError,
                isError = universityError != null,
            )
            RutaUDropdownField(
                selectedOption = campusOptions.firstOrNull { it.id == campus }?.name,
                options = campusOptions.map(CatalogItem::name),
                onOptionSelected = { name ->
                    campusOptions.firstOrNull { it.name == name }?.let { onCampusChange(it.id) }
                },
                label = "Campus o sede habitual",
                modifier = Modifier.fillMaxWidth().testTag("SignUpCampusSelector"),
                enabled = university.isNotBlank(),
                supportingText = campusError ?: if (university.isBlank()) "Primero selecciona tu universidad." else null,
                isError = campusError != null,
            )
            catalogError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            RutaUTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                modifier = Modifier.fillMaxWidth().testTag("SignUpEmailInput"),
                supportingText = emailError,
                isError = emailError != null,
            )
            RutaUPasswordField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth().testTag("SignUpPasswordInput"),
                supportingText = passwordError,
                isError = passwordError != null,
            )
            RutaUPasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar contraseña",
                modifier = Modifier.fillMaxWidth().testTag("SignUpConfirmPasswordInput"),
                supportingText = confirmPasswordError,
                isError = confirmPasswordError != null,
            )
            Text(
                text = "Requisitos: al menos 8 caracteres, una letra mayúscula y un número.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = onTermsAcceptedChange,
                    modifier = Modifier.testTag("SignUpTermsCheckbox"),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acepto los documentos legales de RutaU.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onOpenTerms) {
                            Text("Términos y condiciones")
                        }
                        Text("y", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onOpenPrivacy) {
                            Text("Política de privacidad")
                        }
                    }
                }
            }
            if (termsError != null) {
                Text(
                    text = termsError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            RutaULoadingButton(
                text = "Crear cuenta",
                onClick = onCreateAccount,
                isLoading = isLoading,
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().testTag("SignUpSubmitButton"),
            )
            TextButton(
                onClick = onCreateAccountWithGoogle,
                modifier = Modifier.fillMaxWidth(),
                enabled = isGoogleFormValid && !isLoading,
            ) {
                Text("Registrarme con Google")
            }
            Spacer(Modifier.height(RutaUSpacing.Medium))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    RutaUTheme {
        SignUpScreen(
            fullName = "Mateo Morales Silva",
            university = "university",
            campus = "campus",
            email = "mateo@ejemplo.com",
            password = "RutaU123",
            confirmPassword = "RutaU123",
            photoUrl = null,
            universityOptions = listOf(CatalogItem("university", "Universidad San Carlos")),
            campusOptions = listOf(CatalogItem("campus", "Campus Central")),
            termsAccepted = true,
            onFullNameChange = {},
            onUniversityChange = {},
            onCampusChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPhotoChange = {},
            onTermsAcceptedChange = {},
            onCreateAccount = {},
            onCreateAccountWithGoogle = {},
            onBack = {},
            onOpenTerms = {},
            onOpenPrivacy = {},
            isFormValid = true,
            isGoogleFormValid = true,
        )
    }
}
