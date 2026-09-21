package uvg.edu.rutau.feature.trips

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.navigation.CandidateProfileRoute as CandidateProfileDestination
import uvg.edu.rutau.core.navigation.ConfirmCoordinationRoute as ConfirmCoordinationDestination
import uvg.edu.rutau.core.navigation.MatchesRoute as MatchesDestination
import uvg.edu.rutau.core.navigation.RequestDetailRoute
import uvg.edu.rutau.core.navigation.TripEditorRoute as TripEditorDestination
import uvg.edu.rutau.core.navigation.TripsRoute as TripsDestination

/** Agrega las pantallas de trayectos y compatibilidad a la navegación. */
fun NavGraphBuilder.tripGraph(navController: NavHostController) {
    composable<TripsDestination> {
        TripsRoute(
            onCreateTrip = { navController.navigate(TripEditorDestination()) },
            onEditTrip = { navController.navigate(TripEditorDestination(it)) },
            onOpenMatches = { navController.navigate(MatchesDestination(it)) },
            viewModel = viewModel(factory = TripsViewModel.factory(RutaUAppDependencies.tripRepository)),
        )
    }
    composable<TripEditorDestination> { entry ->
        val route = entry.toRoute<TripEditorDestination>()
        TripEditorRoute(
            onBack = { navController.popBackStack() },
            viewModel = viewModel(
                key = "trip-editor-${route.tripId ?: "new"}",
                factory = TripEditorViewModel.factory(route.tripId, RutaUAppDependencies.tripRepository),
            ),
        )
    }
    composable<MatchesDestination> { entry ->
        val route = entry.toRoute<MatchesDestination>()
        MatchesRoute(
            onBack = { navController.popBackStack() },
            onOpenCandidate = { candidateTripId ->
                navController.navigate(CandidateProfileDestination(route.tripId, candidateTripId))
            },
            viewModel = viewModel(
                key = "matches-${route.tripId}",
                factory = MatchesViewModel.factory(route.tripId, RutaUAppDependencies.tripRepository),
            ),
        )
    }
    composable<CandidateProfileDestination> { entry ->
        val route = entry.toRoute<CandidateProfileDestination>()
        CandidateProfileRoute(
            onBack = { navController.popBackStack() },
            onCoordinate = {
                navController.navigate(ConfirmCoordinationDestination(route.tripId, route.candidateTripId))
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
    composable<ConfirmCoordinationDestination> { entry ->
        val route = entry.toRoute<ConfirmCoordinationDestination>()
        ConfirmCoordinationRoute(
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
}
