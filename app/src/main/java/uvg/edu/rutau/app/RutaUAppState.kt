package uvg.edu.rutau.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uvg.edu.rutau.core.navigation.AccountRoute
import uvg.edu.rutau.core.navigation.AppDestination
import uvg.edu.rutau.core.navigation.RequestsRoute
import uvg.edu.rutau.core.navigation.TripsRoute

/** State holder for app-wide navigation decisions and top-level tab behavior. */
@Stable
class RutaUAppState internal constructor(
    val navController: NavHostController,
) {
    @Composable
    fun currentTopLevelDestination(): RutaUTopLevelDestination? {
        val backStackEntry by navController.currentBackStackEntryAsState()
        return backStackEntry?.destination?.toTopLevelDestination()
    }

    @Composable
    fun shouldShowBottomBar(): Boolean = currentTopLevelDestination() != null

    fun navigateToTopLevel(destination: RutaUTopLevelDestination) {
        val restoredExistingDestination = navController.popBackStack(
            route = destination.route,
            inclusive = false,
            saveState = true,
        )
        if (!restoredExistingDestination) {
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun clearToLogin(destination: AppDestination) {
        navController.navigate(destination) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}

@Composable
fun rememberRutaUAppState(
    navController: NavHostController = rememberNavController(),
): RutaUAppState = remember(navController) { RutaUAppState(navController) }

enum class RutaUTopLevelDestination(
    val route: AppDestination,
) {
    TRIPS(TripsRoute),
    REQUESTS(RequestsRoute),
    ACCOUNT(AccountRoute),
}

private fun NavDestination.toTopLevelDestination(): RutaUTopLevelDestination? {
    val route = route.orEmpty()
    return when {
        route.contains(TripsRoute::class.qualifiedName.orEmpty()) -> RutaUTopLevelDestination.TRIPS
        route.contains(RequestsRoute::class.qualifiedName.orEmpty()) -> RutaUTopLevelDestination.REQUESTS
        route.contains(AccountRoute::class.qualifiedName.orEmpty()) -> RutaUTopLevelDestination.ACCOUNT
        else -> null
    }
}
