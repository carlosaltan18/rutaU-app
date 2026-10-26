package uvg.edu.rutau.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uvg.edu.rutau.core.data.repository.SessionState
import uvg.edu.rutau.core.notifications.PushNavigationStore
import uvg.edu.rutau.core.preferences.PopupMessageHost

/** Inicia la interfaz de RutaU. */
@Composable
fun RutaUApp(
    modifier: Modifier = Modifier,
    appState: RutaUAppState = rememberRutaUAppState(),
) {
    val selectedDestination = appState.currentTopLevelDestination()
    val sessionState = RutaUAppDependencies.sessionRepository.sessionState.collectAsStateWithLifecycle().value
    LaunchedEffect(appState) {
        PushNavigationStore.targets.collect { target -> appState.navigateFromPush(target) }
    }
    LaunchedEffect(appState, RutaUAppDependencies.sessionRepository) {
        RutaUAppDependencies.sessionRepository.sessionState.collect { state ->
            if (state is SessionState.SignedOut) appState.clearToLogin()
        }
    }

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
    if (sessionState is SessionState.SignedIn) {
        PopupMessageHost(
            RutaUAppDependencies.popupMessageFeedRepository,
            RutaUAppDependencies.popupMessageRepository,
        )
    }
}
