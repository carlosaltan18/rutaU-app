package uvg.edu.rutau.feature.requests

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.navigation.CoordinatedRideRoute as CoordinatedRideDestination
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
            onBack = { navController.popBackStack() },
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
                ),
            ),
        )
    }
}
