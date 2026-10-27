package uvg.edu.rutau.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uvg.edu.rutau.core.data.repository.SessionState
import uvg.edu.rutau.core.notifications.PushNavigationStore
import uvg.edu.rutau.core.notifications.PushPopupHost
import uvg.edu.rutau.core.notifications.PushPopupMessage
import uvg.edu.rutau.core.notifications.PushPopupStore
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
    var pushPopup by remember { mutableStateOf<PushPopupMessage?>(null) }
    val themeMode = RutaUAppDependencies.themePreferenceRepository
        .observeThemeMode()
        .collectAsStateWithLifecycle(ThemeMode.SYSTEM)
        .value
    LaunchedEffect(appState) {
        PushNavigationStore.targets.collect { target -> appState.navigateFromPush(target) }
    }
    LaunchedEffect(Unit) {
        PushPopupStore.messages.collect { message -> pushPopup = message }
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
        pushPopup?.let { message ->
            PushPopupHost(
                message = message,
                onDismiss = { pushPopup = null },
                onOpen = { target ->
                    pushPopup = null
                    appState.navigateFromPush(target)
                },
            )
        }
        if (sessionState is SessionState.SignedIn && pushPopup == null) {
            PopupMessageHost(
                RutaUAppDependencies.popupMessageFeedRepository,
                RutaUAppDependencies.popupMessageRepository,
            )
        }
    }
}
