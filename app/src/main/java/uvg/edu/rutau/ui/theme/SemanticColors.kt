package uvg.edu.rutau.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores de estado que Material 3 no representa como roles de color propios. */
data class RutaUSemanticColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val LocalRutaUSemanticColors = staticCompositionLocalOf {
    RutaUSemanticColors(
        success = RutaUSuccess,
        successContainer = RutaUSuccessContainer,
        onSuccessContainer = RutaUOnSuccessContainer,
        warning = RutaUWarning,
        warningContainer = RutaUWarningContainer,
        onWarningContainer = RutaUOnWarningContainer,
    )
}
