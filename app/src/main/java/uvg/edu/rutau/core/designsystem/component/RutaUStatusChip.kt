package uvg.edu.rutau.core.designsystem.component

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Variantes semánticas disponibles para los chips de estado. */
enum class RutaUStatusType {
    NEUTRAL,
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

/** Chip que comunica un estado mediante texto, color y un icono opcional. */
@Composable
fun RutaUStatusChip(
    label: String,
    type: RutaUStatusType,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val (containerColor, contentColor) = when (type) {
        RutaUStatusType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
        RutaUStatusType.INFO -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        RutaUStatusType.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        RutaUStatusType.WARNING -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        RutaUStatusType.ERROR -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }

    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            leadingIconContentColor = contentColor,
        ),
    )
}
