package uvg.edu.rutau.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import uvg.edu.rutau.core.navigation.AccountRoute as AccountDestination
import uvg.edu.rutau.core.navigation.CandidateProfileRoute
import uvg.edu.rutau.core.navigation.ConfirmCoordinationRoute
import uvg.edu.rutau.core.navigation.EmailSentRoute as EmailSentDestination
import uvg.edu.rutau.core.navigation.LoginRoute as LoginDestination
import uvg.edu.rutau.core.navigation.MatchesRoute
import uvg.edu.rutau.core.navigation.RecoverAccessRoute as RecoverAccessDestination
import uvg.edu.rutau.core.navigation.RequestDetailRoute
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
import uvg.edu.rutau.feature.trips.CandidateProfileRoute as CandidateProfileScreen
import uvg.edu.rutau.feature.trips.CandidateProfileViewModel
import uvg.edu.rutau.feature.trips.ConfirmCoordinationRoute as ConfirmCoordinationScreen
import uvg.edu.rutau.feature.trips.ConfirmCoordinationViewModel
import uvg.edu.rutau.feature.trips.MatchesRoute as MatchesScreen
import uvg.edu.rutau.feature.trips.MatchesViewModel
import uvg.edu.rutau.feature.trips.TripEditorRoute as TripEditorScreen
import uvg.edu.rutau.feature.trips.TripEditorViewModel
import uvg.edu.rutau.feature.trips.TripsRoute as TripsScreen
import uvg.edu.rutau.feature.trips.TripsViewModel
import uvg.edu.rutau.feature.requests.requestGraph

/** Conecta las pantallas de RutaU y permite pasar de una a otra. */
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
        composable<TripsRoute> {
            TripsScreen(
                onCreateTrip = { navController.navigate(TripEditorRoute()) },
                onEditTrip = { navController.navigate(TripEditorRoute(it)) },
                onOpenMatches = { navController.navigate(MatchesRoute(it)) },
                viewModel = viewModel(factory = TripsViewModel.factory(RutaUAppDependencies.tripRepository)),
            )
        }
        composable<TripEditorRoute> { entry ->
            val route = entry.toRoute<TripEditorRoute>()
            TripEditorScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel(
                    key = "trip-editor-${route.tripId ?: "new"}",
                    factory = TripEditorViewModel.factory(route.tripId, RutaUAppDependencies.tripRepository),
                ),
            )
        }
        composable<MatchesRoute> { entry ->
            val route = entry.toRoute<MatchesRoute>()
            MatchesScreen(
                onBack = { navController.popBackStack() },
                onOpenCandidate = { candidateTripId ->
                    navController.navigate(CandidateProfileRoute(route.tripId, candidateTripId))
                },
                viewModel = viewModel(
                    key = "matches-${route.tripId}",
                    factory = MatchesViewModel.factory(route.tripId, RutaUAppDependencies.tripRepository),
                ),
            )
        }
        composable<CandidateProfileRoute> { entry ->
            val route = entry.toRoute<CandidateProfileRoute>()
            CandidateProfileScreen(
                onBack = { navController.popBackStack() },
                onCoordinate = {
                    navController.navigate(ConfirmCoordinationRoute(route.tripId, route.candidateTripId))
                },
                viewModel = viewModel(
                    key = "candidate-${route.tripId}-${route.candidateTripId}",
                    factory = CandidateProfileViewModel.factory(
                        route.tripId,
                        route.candidateTripId,
                        RutaUAppDependencies.tripRepository,
                    ),
                ),
            )
        }
        composable<ConfirmCoordinationRoute> { entry ->
            val route = entry.toRoute<ConfirmCoordinationRoute>()
            ConfirmCoordinationScreen(
                onBack = { navController.popBackStack() },
                onConfirmed = { requestId -> navController.navigate(RequestDetailRoute(requestId)) },
                viewModel = viewModel(
                    key = "confirm-${route.tripId}-${route.candidateTripId}",
                    factory = ConfirmCoordinationViewModel.factory(
                        route.tripId,
                        route.candidateTripId,
                        RutaUAppDependencies.tripRepository,
                        RutaUAppDependencies.coordinationRepository,
                    ),
                ),
            )
        }
        requestGraph(navController)
        composable<AccountDestination> {
            AccountRoute(
                onSignedOut = {
                    appState.clearToLogin()
                },
            )
        }
    }
}
