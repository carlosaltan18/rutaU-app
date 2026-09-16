package uvg.edu.rutau.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

/** Punto de entrada Compose de RutaU. */
@Composable
fun RutaUApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    RutaUNavHost(
        navController = navController,
        modifier = modifier,
    )
}
