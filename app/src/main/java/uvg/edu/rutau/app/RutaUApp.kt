package uvg.edu.rutau.app

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

/** Compose entry point for RutaU. */
@Composable
fun RutaUApp(modifier: Modifier = Modifier) {
    val appState = rememberRutaUAppState()
    val selectedDestination = appState.currentTopLevelDestination()

    RutaUScaffold(
        selectedDestination = selectedDestination,
        onDestinationSelected = appState::navigateToTopLevel,
        modifier = modifier,
    ) { innerPadding ->
        RutaUNavHost(
            appState = appState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
