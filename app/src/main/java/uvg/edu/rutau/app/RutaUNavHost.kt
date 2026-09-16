package uvg.edu.rutau.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.navigation.AccountRoute as AccountDestination
import uvg.edu.rutau.core.navigation.CandidateProfileRoute
import uvg.edu.rutau.core.navigation.ConfirmCoordinationRoute
import uvg.edu.rutau.core.navigation.CoordinatedRideRoute
import uvg.edu.rutau.core.navigation.EmailSentRoute as EmailSentDestination
import uvg.edu.rutau.core.navigation.LoginRoute as LoginDestination
import uvg.edu.rutau.core.navigation.MatchesRoute
import uvg.edu.rutau.core.navigation.RecoverAccessRoute as RecoverAccessDestination
import uvg.edu.rutau.core.navigation.RequestDetailRoute
import uvg.edu.rutau.core.navigation.RequestsRoute
import uvg.edu.rutau.core.navigation.ResetPasswordRoute as ResetPasswordDestination
import uvg.edu.rutau.core.navigation.SignUpRoute as SignUpDestination
import uvg.edu.rutau.core.navigation.TripEditorRoute
import uvg.edu.rutau.core.navigation.TripsRoute
import uvg.edu.rutau.feature.auth.LoginRoute
import uvg.edu.rutau.feature.auth.RecoverAccessRoute
import uvg.edu.rutau.feature.auth.SignUpRoute
import uvg.edu.rutau.feature.auth.EmailSentRoute
import uvg.edu.rutau.feature.auth.PasswordRecoveryViewModel
import uvg.edu.rutau.feature.auth.ResetPasswordRoute
import uvg.edu.rutau.feature.account.AccountRoute

/**
 * Central navigation host. Each module replaces its temporary destination with its Route.
 */
@Composable
fun RutaUNavHost(
    appState: RutaUAppState,
    modifier: Modifier = Modifier,
    startDestination: Any = LoginDestination,
) {
    val navController = appState.navController
    val passwordRecoveryViewModel: PasswordRecoveryViewModel = viewModel(
        factory = PasswordRecoveryViewModel.factory(RutaUAppDependencies.sessionRepository),
    )
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<LoginDestination> {
            LoginRoute(
                onLoginSuccess = {
                    appState.navigateToAuthenticatedRoot()
                },
                onNavigateToSignUp = { navController.navigate(SignUpDestination) },
                onNavigateToRecovery = { navController.navigate(RecoverAccessDestination) },
            )
        }
        composable<SignUpDestination> {
            SignUpRoute(
                onAccountCreated = {
                    appState.navigateToAuthenticatedRoot()
                },
                onBack = { navController.popBackStack() },
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
                onBackToLogin = {
                    appState.clearToLogin()
                },
                viewModel = passwordRecoveryViewModel,
            )
        }
        composable<ResetPasswordDestination> {
            ResetPasswordRoute(
                onPasswordReset = {
                    appState.clearToLogin()
                },
                onBackToLogin = {
                    appState.clearToLogin()
                },
            )
        }
        composable<TripsRoute> { TemporaryDestination("Mis trayectos") }
        composable<TripEditorRoute> { TemporaryDestination("Crear o editar trayecto") }
        composable<MatchesRoute> { TemporaryDestination("Compañeros compatibles") }
        composable<CandidateProfileRoute> { TemporaryDestination("Perfil compatible") }
        composable<ConfirmCoordinationRoute> { TemporaryDestination("Confirmar coordinación") }
        composable<RequestsRoute> { TemporaryDestination("Solicitudes") }
        composable<RequestDetailRoute> { TemporaryDestination("Detalle de solicitud") }
        composable<CoordinatedRideRoute> { TemporaryDestination("Viaje coordinado") }
        composable<AccountDestination> {
            AccountRoute(
                onSignedOut = {
                    appState.clearToLogin()
                },
            )
        }
    }
}

@Composable
private fun TemporaryDestination(title: String) {
    RutaUScreenContainer(
        topBar = { RutaUTopAppBar(title = title) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
