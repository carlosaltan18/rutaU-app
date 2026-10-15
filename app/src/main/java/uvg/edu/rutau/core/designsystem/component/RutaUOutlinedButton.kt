package uvg.edu.rutau.core.designsystem.component

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Acción alternativa con menor énfasis visual que la acción principal. */
@Composable
fun RutaUOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
