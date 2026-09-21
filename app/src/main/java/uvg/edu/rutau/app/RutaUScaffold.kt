package uvg.edu.rutau.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Coloca la barra inferior y el espacio común de la aplicación. */
@Composable
fun RutaUScaffold(
    selectedDestination: RutaUTopLevelDestination?,
    onDestinationSelected: (RutaUTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = {
            selectedDestination?.let { destination ->
                RutaUBottomNavigation(
                    selectedDestination = destination,
                    onDestinationSelected = onDestinationSelected,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun RutaUScaffoldPreview() {
    RutaUTheme {
        RutaUScaffold(
            selectedDestination = RutaUTopLevelDestination.TRIPS,
            onDestinationSelected = {},
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}
