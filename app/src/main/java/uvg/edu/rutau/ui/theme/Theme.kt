package uvg.edu.rutau.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = RutaUPrimaryContainer,
    onPrimary = RutaUOnPrimaryContainer,
    primaryContainer = RutaUPrimary,
    onPrimaryContainer = RutaUOnPrimary,
    secondary = RutaUSecondaryContainer,
    onSecondary = RutaUOnSecondaryContainer,
    tertiary = RutaUTertiaryContainer,
    onTertiary = RutaUOnTertiaryContainer,
    background = RutaUDarkBackground,
    onBackground = RutaUDarkOnSurface,
    surface = RutaUDarkSurface,
    onSurface = RutaUDarkOnSurface,
    onSurfaceVariant = RutaUSecondaryContainer,
    outline = RutaUOutline,
    error = RutaUError,
    onError = RutaUOnError,
    errorContainer = RutaUErrorContainer,
    onErrorContainer = RutaUOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = RutaUPrimary,
    onPrimary = RutaUOnPrimary,
    primaryContainer = RutaUPrimaryContainer,
    onPrimaryContainer = RutaUOnPrimaryContainer,
    secondary = RutaUSecondary,
    onSecondary = RutaUOnSecondary,
    secondaryContainer = RutaUSecondaryContainer,
    onSecondaryContainer = RutaUOnSecondaryContainer,
    tertiary = RutaUTertiary,
    onTertiary = RutaUOnTertiary,
    tertiaryContainer = RutaUTertiaryContainer,
    onTertiaryContainer = RutaUOnTertiaryContainer,
    background = RutaUBackground,
    onBackground = RutaUOnSurface,
    surface = RutaUSurface,
    onSurface = RutaUOnSurface,
    onSurfaceVariant = RutaUOnSurfaceVariant,
    outline = RutaUOutline,
    error = RutaUError,
    onError = RutaUOnError,
    errorContainer = RutaUErrorContainer,
    onErrorContainer = RutaUOnErrorContainer,
)

@Composable
fun RutaUTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalRutaUSemanticColors provides RutaUSemanticColors(
            success = RutaUSuccess,
            successContainer = RutaUSuccessContainer,
            onSuccessContainer = RutaUOnSuccessContainer,
            warning = RutaUWarning,
            warningContainer = RutaUWarningContainer,
            onWarningContainer = RutaUOnWarningContainer,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = RutaUShapes,
            content = content,
        )
    }
}
