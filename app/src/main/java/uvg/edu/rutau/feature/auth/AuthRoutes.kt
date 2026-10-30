package uvg.edu.rutau.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.auth.findActivity
import uvg.edu.rutau.core.navigation.LegalDocumentType
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Conecta el inicio de sesión con sus datos y acciones. */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.factory(RutaUAppDependencies.sessionRepository),
    ),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event is LoginEvent.SignedIn) onLoginSuccess()
        }
    }
    LoginScreen(
        email = state.email,
        password = state.password,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
        onGoogleLogin = {
            val activity = context.findActivity()
            if (activity == null) {
                viewModel.loginWithGoogle(null)
            } else {
                scope.launch {
                    viewModel.loginWithGoogle(
                        RutaUAppDependencies.googleSignInCoordinator.requestIdToken(activity),
                    )
                }
            }
        },
        onForgotPassword = onNavigateToRecovery,
        onSignUp = onNavigateToSignUp,
        onOpenTerms = { onOpenLegalDocument(LegalDocumentType.TERMS) },
        onOpenPrivacy = { onOpenLegalDocument(LegalDocumentType.PRIVACY) },
        modifier = modifier,
        isLoading = state.isLoading,
        emailError = state.emailError,
        passwordError = state.passwordError,
        formError = state.formError,
    )
}

/** Conecta el registro con sus datos y acciones. */
@Composable
fun SignUpRoute(
    onAccountCreated: () -> Unit,
    onBack: () -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.factory(
            RutaUAppDependencies.sessionRepository,
            RutaUAppDependencies.catalogRepository,
        ),
    ),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event is SignUpEvent.AccountCreated) onAccountCreated()
        }
    }
    SignUpScreen(
        fullName = state.fullName,
        university = state.university,
        campus = state.campus,
        email = state.email,
        password = state.password,
        confirmPassword = state.confirmPassword,
        photoUrl = state.photoUrl,
        universityOptions = state.universities,
        campusOptions = state.campuses,
        termsAccepted = state.termsAccepted,
        onFullNameChange = viewModel::onFullNameChange,
        onUniversityChange = viewModel::onUniversityChange,
        onCampusChange = viewModel::onCampusChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onPhotoChange = viewModel::onPhotoChange,
        onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
        onCreateAccount = viewModel::createAccount,
        onCreateAccountWithGoogle = {
            val activity = context.findActivity()
            if (activity == null) {
                viewModel.createAccountWithGoogle(null)
            } else {
                scope.launch {
                    viewModel.createAccountWithGoogle(
                        RutaUAppDependencies.googleSignInCoordinator.requestIdToken(activity),
                    )
                }
            }
        },
        onBack = onBack,
        onOpenTerms = { onOpenLegalDocument(LegalDocumentType.TERMS) },
        onOpenPrivacy = { onOpenLegalDocument(LegalDocumentType.PRIVACY) },
        modifier = modifier,
        isLoading = state.isLoading,
        isFormValid = state.isFormValid,
        isGoogleFormValid = state.isGoogleFormValid,
        fullNameError = state.fullNameError,
        universityError = state.universityError,
        campusError = state.campusError,
        emailError = state.emailError,
        passwordError = state.passwordError,
        confirmPasswordError = state.confirmPasswordError,
        termsError = state.termsError,
        catalogError = state.catalogError,
    )
}

/** Conecta la recuperación de acceso con sus datos y acciones. */
@Composable
fun RecoverAccessRoute(
    onInstructionsSent: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordRecoveryViewModel = viewModel(
        factory = PasswordRecoveryViewModel.factory(RutaUAppDependencies.sessionRepository),
    ),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event is PasswordRecoveryEvent.InstructionsSent) onInstructionsSent()
        }
    }
    RecoverAccessScreen(
        email = state.email,
        onEmailChange = viewModel::onEmailChange,
        onSendInstructions = viewModel::sendInstructions,
        onBack = onBack,
        modifier = modifier,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
    )
}

/** Permite reenviar las instrucciones para recuperar la contraseña. */
@Composable
fun EmailSentRoute(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordRecoveryViewModel = viewModel(
        factory = PasswordRecoveryViewModel.factory(RutaUAppDependencies.sessionRepository),
    ),
) {
    EmailSentScreen(
        onResend = viewModel::resendInstructions,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

/** Conecta el cambio de contraseña con sus datos y acciones. */
@Composable
fun ResetPasswordRoute(
    onPasswordReset: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = viewModel(
        factory = ResetPasswordViewModel.factory(RutaUAppDependencies.sessionRepository),
    ),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event is ResetPasswordEvent.PasswordReset) onPasswordReset()
        }
    }
    ResetPasswordScreen(
        newPassword = state.newPassword,
        confirmPassword = state.confirmPassword,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onUpdatePassword = viewModel::resetPassword,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginRoutePreview() {
    RutaUTheme {
        LoginRoute(
            onLoginSuccess = {},
            onNavigateToSignUp = {},
            onNavigateToRecovery = {},
            onOpenLegalDocument = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpRoutePreview() {
    RutaUTheme {
        SignUpRoute(onAccountCreated = {}, onBack = {}, onOpenLegalDocument = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoverAccessRoutePreview() {
    RutaUTheme {
        RecoverAccessRoute(onInstructionsSent = {}, onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailSentRoutePreview() {
    RutaUTheme {
        EmailSentRoute(
            onBackToLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordRoutePreview() {
    RutaUTheme {
        ResetPasswordRoute(onPasswordReset = {}, onBackToLogin = {})
    }
}
