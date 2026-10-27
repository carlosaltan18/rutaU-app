package uvg.edu.rutau.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uvg.edu.rutau.core.data.repository.SessionState
import uvg.edu.rutau.core.notifications.PushNavigationStore
import uvg.edu.rutau.core.preferences.PopupMessageHost
import uvg.edu.rutau.core.preferences.ThemeMode
import uvg.edu.rutau.ui.theme.RutaUTheme
import androidx.compose.foundation.isSystemInDarkTheme

/** Inicia la interfaz de RutaU. */
@Composable
fun RutaUApp(
    modifier: Modifier = Modifier,
    appState: RutaUAppState = rememberRutaUAppState(),
) {
    val selectedDestination = appState.currentTopLevelDestination()
    val sessionState = RutaUAppDependencies.sessionRepository.sessionState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val themeMode = RutaUAppDependencies.themePreferenceRepository
        .observeThemeMode()
        .collectAsStateWithLifecycle(ThemeMode.SYSTEM)
        .value
    LaunchedEffect(appState) {
        PushNavigationStore.targets.collect { target -> appState.navigateFromPush(target) }
    }
    LaunchedEffect(appState, RutaUAppDependencies.sessionRepository) {
        RutaUAppDependencies.sessionRepository.sessionState.collect { state ->
            when (state) {
                is SessionState.SignedIn -> appState.navigateToAuthenticatedRoot()
                SessionState.SignedOut -> appState.clearToLogin()
            }
        }
    }
    LaunchedEffect(sessionState) {
        if (
            sessionState is SessionState.SignedIn &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    RutaUTheme(darkTheme = darkTheme) {
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
}
