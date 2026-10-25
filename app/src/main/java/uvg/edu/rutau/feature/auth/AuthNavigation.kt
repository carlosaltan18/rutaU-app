package uvg.edu.rutau.feature.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import uvg.edu.rutau.core.navigation.EmailSentRoute as EmailSentDestination
import uvg.edu.rutau.core.navigation.LoginRoute as LoginDestination
import uvg.edu.rutau.core.navigation.LegalDocumentRoute as LegalDocumentDestination
import uvg.edu.rutau.core.navigation.LegalDocumentType
import uvg.edu.rutau.core.navigation.RecoverAccessRoute as RecoverAccessDestination
import uvg.edu.rutau.core.navigation.ResetPasswordRoute as ResetPasswordDestination
import uvg.edu.rutau.core.navigation.SignUpRoute as SignUpDestination

/** Agrega las pantallas de acceso y cuenta nueva a la navegación. */
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    passwordRecoveryViewModel: PasswordRecoveryViewModel,
    onAuthenticated: () -> Unit,
    onClearToLogin: () -> Unit,
) {
    composable<LoginDestination> {
        LoginRoute(
            onLoginSuccess = onAuthenticated,
            onNavigateToSignUp = { navController.navigate(SignUpDestination) },
            onNavigateToRecovery = { navController.navigate(RecoverAccessDestination) },
            onOpenLegalDocument = { navController.navigate(LegalDocumentDestination(it)) },
        )
    }
    composable<SignUpDestination> {
        SignUpRoute(
            onAccountCreated = onAuthenticated,
            onBack = { navController.popBackStack() },
            onOpenLegalDocument = { navController.navigate(LegalDocumentDestination(it)) },
        )
    }
    composable<RecoverAccessDestination> {
        RecoverAccessRoute(
            onInstructionsSent = { navController.navigate(EmailSentDestination) },
            onBack = { navController.popBackStack() },
            viewModel = passwordRecoveryViewModel,
        )
    }
    composable<EmailSentDestination> {
        EmailSentRoute(
            onOpenResetLink = { navController.navigate(ResetPasswordDestination) },
            onBackToLogin = onClearToLogin,
            viewModel = passwordRecoveryViewModel,
        )
    }
    composable<ResetPasswordDestination> {
        ResetPasswordRoute(
            onPasswordReset = onClearToLogin,
            onBackToLogin = onClearToLogin,
        )
    }
}
