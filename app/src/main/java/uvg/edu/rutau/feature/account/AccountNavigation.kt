package uvg.edu.rutau.feature.account

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import uvg.edu.rutau.core.navigation.AccountRoute as AccountDestination
import uvg.edu.rutau.core.navigation.LegalDocumentRoute as LegalDocumentDestination

/** Agrega la pantalla de cuenta a la navegación. */
fun NavGraphBuilder.accountGraph(
    navController: NavHostController,
    onSignedOut: () -> Unit,
) {
    composable<AccountDestination> {
        AccountRoute(
            onSignedOut = onSignedOut,
            onOpenLegalDocument = { navController.navigate(LegalDocumentDestination(it)) },
        )
    }
    composable<LegalDocumentDestination> { entry ->
        LegalDocumentScreen(
            documentType = entry.toRoute<LegalDocumentDestination>().document,
            onBack = { navController.popBackStack() },
        )
    }
}
