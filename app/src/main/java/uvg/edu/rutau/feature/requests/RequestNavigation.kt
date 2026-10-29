package uvg.edu.rutau.feature.requests

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.navigation.CoordinatedRideRoute as CoordinatedRideDestination
import uvg.edu.rutau.core.navigation.CoordinationDetailRoute as CoordinationDetailDestination
import uvg.edu.rutau.core.navigation.MatchesRoute as MatchesDestination
import uvg.edu.rutau.core.navigation.RequestDetailRoute as RequestDetailDestination
import uvg.edu.rutau.core.navigation.RequestsRoute as RequestsDestination

/** Agrega las pantallas de solicitudes al conjunto de rutas de la aplicación. */
fun NavGraphBuilder.requestGraph(navController: NavHostController) {
    composable<RequestsDestination> {
        RequestsRoute(
            onOpenRequest = { requestId -> navController.navigate(RequestDetailDestination(requestId)) },
            viewModel = viewModel(
                factory = RequestsViewModel.factory(
                    RutaUAppDependencies.rideRequestRepository,
                    RutaUAppDependencies.userRepository,
                ),
            ),
        )
    }
    composable<RequestDetailDestination> { entry ->
        val route = entry.toRoute<RequestDetailDestination>()
        RequestDetailRoute(
            onBack = { navController.openRequestsHome() },
            onOpenCoordinatedRide = {
                navController.navigate(CoordinatedRideDestination(route.requestId))
            },
            onSearchAgain = { tripId -> navController.navigate(MatchesDestination(tripId)) },
            viewModel = viewModel(
                key = "request-detail-${route.requestId}",
                factory = RequestDetailViewModel.factory(
                    route.requestId,
                    RutaUAppDependencies.rideRequestRepository,
                    RutaUAppDependencies.coordinationRepository,
                    RutaUAppDependencies.userRepository,
                ),
            ),
        )
    }
    composable<CoordinatedRideDestination> { entry ->
        val route = entry.toRoute<CoordinatedRideDestination>()
        CoordinatedRideRoute(
            onBack = { navController.popBackStack() },
            viewModel = viewModel(
                key = "coordinated-ride-${route.requestId}",
                factory = CoordinatedRideViewModel.factory(
                    route.requestId,
                    RutaUAppDependencies.coordinationRepository,
                    RutaUAppDependencies.userRepository,
                    RutaUAppDependencies.rideRequestRepository,
                    RutaUAppDependencies.liveTripLocationRepository,
                ),
            ),
        )
    }
    composable<CoordinationDetailDestination> { entry ->
        val route = entry.toRoute<CoordinationDetailDestination>()
        val viewModel: CoordinationRedirectViewModel = viewModel(
            key = "coordination-redirect-${route.coordinationId}",
            factory = CoordinationRedirectViewModel.factory(
                route.coordinationId,
                RutaUAppDependencies.coordinationRepository,
            ),
        )
        val requestId by viewModel.requestId.collectAsStateWithLifecycle()
        LaunchedEffect(requestId) {
            requestId?.let { resolvedRequestId ->
                navController.navigate(CoordinatedRideDestination(resolvedRequestId)) {
                    popUpTo(CoordinationDetailDestination(route.coordinationId)) { inclusive = true }
                }
            }
        }
        Box(Modifier.fillMaxSize())
    }
}

/** Vuelve a la lista principal sin regresar a formularios que ya terminaron. */
private fun NavHostController.openRequestsHome() {
    navigate(RequestsDestination) {
        popUpTo<RequestsDestination> { inclusive = false }
        launchSingleTop = true
    }
}
