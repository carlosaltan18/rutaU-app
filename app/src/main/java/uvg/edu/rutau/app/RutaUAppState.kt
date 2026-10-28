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
import uvg.edu.rutau.core.navigation.LoginRoute
import uvg.edu.rutau.core.navigation.RequestsRoute
import uvg.edu.rutau.core.navigation.TripsRoute
import uvg.edu.rutau.core.navigation.RequestDetailRoute
import uvg.edu.rutau.core.navigation.CoordinatedRideRoute
import uvg.edu.rutau.core.navigation.CoordinationDetailRoute
import uvg.edu.rutau.core.notifications.PushNavigationTarget
import uvg.edu.rutau.core.preferences.PopupMessageAction

/** Maneja la navegación principal de la aplicación. */
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

    fun navigateToAuthenticatedRoot() {
        navController.navigate(TripsRoute) {
            popUpTo(LoginRoute) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun clearToLogin() {
        navController.navigate(LoginRoute) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    /** Abre el recurso indicado por un push después de volver a consultar la API. */
    fun navigateFromPush(target: PushNavigationTarget) {
        when (target.destination) {
            "RIDE_REQUEST_DETAIL" -> navController.navigate(RequestDetailRoute(target.resourceId))
            "COORDINATION_DETAIL" -> navController.navigate(CoordinationDetailRoute(target.resourceId))
        }
    }

    /** Abre una sección principal cuando el anuncio de Firestore lo solicita. */
    fun navigateFromPopup(action: PopupMessageAction, actionValue: String?) {
        when (action) {
            PopupMessageAction.OPEN_PROFILE -> navigateToTopLevel(RutaUTopLevelDestination.ACCOUNT)
            PopupMessageAction.OPEN_TRIPS -> navigateToTopLevel(RutaUTopLevelDestination.TRIPS)
            PopupMessageAction.OPEN_REQUESTS -> navigateToTopLevel(RutaUTopLevelDestination.REQUESTS)
            PopupMessageAction.NONE -> Unit
        }
    }
}

@Composable
fun rememberRutaUAppState(
    navController: NavHostController = rememberNavController(),
): RutaUAppState = remember(navController) { RutaUAppState(navController) }

/** Indica las tres secciones principales después de iniciar sesión. */
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
