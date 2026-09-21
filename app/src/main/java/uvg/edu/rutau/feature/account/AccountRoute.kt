package uvg.edu.rutau.feature.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import uvg.edu.rutau.app.RutaUAppDependencies
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Conecta la pantalla de cuenta con sus datos y acciones. */
@Composable
fun AccountRoute(
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.factory(
            userRepository = RutaUAppDependencies.userRepository,
            sessionRepository = RutaUAppDependencies.sessionRepository,
        ),
    ),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
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
            viewModel = AccountViewModel(
                userRepository = RutaUAppDependencies.userRepository,
                sessionRepository = RutaUAppDependencies.sessionRepository,
            ),
        )
    }
}
