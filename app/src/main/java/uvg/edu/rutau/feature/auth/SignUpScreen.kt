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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.data.catalog.AcademicCatalog
import uvg.edu.rutau.core.designsystem.component.RutaULoadingButton
import uvg.edu.rutau.core.designsystem.component.RutaUDropdownField
import uvg.edu.rutau.core.designsystem.component.RutaUPasswordField
import uvg.edu.rutau.core.designsystem.component.RutaUProfilePhotoPicker
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTextField
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Pantalla pura de creación de cuenta, sin verificación institucional. */
@Composable
fun SignUpScreen(
    fullName: String,
    university: String,
    campus: String,
    email: String,
    password: String,
    confirmPassword: String,
    photoUrl: String?,
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
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
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Medium),
        ) {
            Spacer(Modifier.height(RutaUSpacing.Small))
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
                modifier = Modifier.fillMaxWidth(),
            )
            RutaUProfilePhotoPicker(
                fullName = fullName,
                photoUrl = photoUrl,
                onPhotoSelected = onPhotoChange,
                modifier = Modifier.fillMaxWidth(),
            )
            RutaUDropdownField(
                selectedOption = university.ifBlank { null },
                options = AcademicCatalog.universities,
                onOptionSelected = onUniversityChange,
                label = "Universidad",
                modifier = Modifier.fillMaxWidth(),
            )
            RutaUDropdownField(
                selectedOption = campus.ifBlank { null },
                options = AcademicCatalog.campusesFor(university),
                onOptionSelected = onCampusChange,
                label = "Campus o sede habitual",
                modifier = Modifier.fillMaxWidth(),
                enabled = university.isNotBlank(),
                supportingText = if (university.isBlank()) "Primero selecciona tu universidad." else null,
            )
            RutaUTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                modifier = Modifier.fillMaxWidth(),
            )
            RutaUPasswordField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth(),
            )
            RutaUPasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar contraseña",
                modifier = Modifier.fillMaxWidth(),
                supportingText = errorMessage,
                isError = errorMessage != null,
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
                )
                Text(
                    text = "Acepto los Términos y condiciones y la Política de privacidad de RutaU.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            RutaULoadingButton(
                text = "Crear cuenta",
                onClick = onCreateAccount,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(RutaUSpacing.Large))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    RutaUTheme {
        SignUpScreen(
            fullName = "Mateo Morales Silva",
            university = "Universidad San Carlos",
            campus = "Campus Central",
            email = "mateo@ejemplo.com",
            password = "RutaU123",
            confirmPassword = "RutaU123",
            photoUrl = null,
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
            onBack = {},
        )
    }
}
