package uvg.edu.rutau.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.lifecycle.viewmodel.compose.viewModel
import uvg.edu.rutau.core.navigation.LoginRoute as LoginDestination
import uvg.edu.rutau.feature.auth.PasswordRecoveryViewModel
import uvg.edu.rutau.feature.account.accountGraph
import uvg.edu.rutau.feature.auth.authGraph
import uvg.edu.rutau.feature.requests.requestGraph
import uvg.edu.rutau.feature.trips.tripGraph

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
        authGraph(
            navController = navController,
            passwordRecoveryViewModel = passwordRecoveryViewModel,
            onAuthenticated = appState::navigateToAuthenticatedRoot,
            onClearToLogin = appState::clearToLogin,
        )
        tripGraph(navController)
        requestGraph(navController)
        accountGraph(onSignedOut = appState::clearToLogin)
    }
}
