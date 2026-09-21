package uvg.edu.rutau.feature.account

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import uvg.edu.rutau.core.navigation.AccountRoute as AccountDestination

/** Agrega la pantalla de cuenta a la navegación. */
fun NavGraphBuilder.accountGraph(
    onSignedOut: () -> Unit,
) {
    composable<AccountDestination> {
        AccountRoute(onSignedOut = onSignedOut)
    }
}
