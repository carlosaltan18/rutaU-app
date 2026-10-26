package uvg.edu.rutau.core.location

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Prepara la petición de ubicación cuando una persona inicia un viaje confirmado. */
@Composable
fun rememberLiveTripLocationPermissionRequester(
    onResult: (Boolean) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        onResult(grants.values.any { it })
    }
    return remember(launcher) {
        { launcher.launch(LiveTripLocationPermissions.foregroundPermissions) }
    }
}
