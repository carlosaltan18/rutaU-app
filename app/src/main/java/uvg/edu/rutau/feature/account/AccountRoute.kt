package uvg.edu.rutau.feature.account

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.core.navigation.LegalDocumentType
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Conecta la pantalla de cuenta con sus datos y acciones. */
@Composable
fun AccountRoute(
    onSignedOut: () -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.factory(
            userRepository = RutaUAppDependencies.userRepository,
            sessionRepository = RutaUAppDependencies.sessionRepository,
            popupMessageRepository = RutaUAppDependencies.popupMessageRepository,
            themePreferenceRepository = RutaUAppDependencies.themePreferenceRepository,
            catalogRepository = RutaUAppDependencies.catalogRepository,
        ),
    ),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event is AccountEvent.SignedOut) onSignedOut()
        }
    }
    if (state == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        AccountScreen(
            state = state,
            onAction = viewModel::onAction,
            onOpenLegalDocument = onOpenLegalDocument,
            onRequestNotificationPermission = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountRoutePreview() {
    RutaUTheme {
        AccountRoute(
            onSignedOut = {},
            onOpenLegalDocument = {},
        )
    }
}
