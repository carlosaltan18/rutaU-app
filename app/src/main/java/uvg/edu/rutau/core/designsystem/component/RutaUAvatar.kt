package uvg.edu.rutau.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Avatar circular que muestra foto remota o iniciales cuando no hay imagen disponible. */
@Composable
fun RutaUAvatar(
    fullName: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)

    if (photoUrl.isNullOrBlank()) {
        Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fullName.initials(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Foto de $fullName",
            modifier = avatarModifier,
            contentScale = ContentScale.Crop,
        )
    }
}

private fun String.initials(): String =
    trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }
        .ifBlank { "?" }
