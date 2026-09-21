package uvg.edu.rutau.core.designsystem.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Permite elegir una foto opcional para el perfil. */
@Composable
fun RutaUProfilePhotoPicker(
    fullName: String,
    photoUrl: String?,
    onPhotoSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Foto de perfil (opcional)",
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) onPhotoSelected(uri.toString())
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        RutaUAvatar(fullName = fullName, photoUrl = photoUrl, size = 72.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge)
            TextButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (photoUrl == null) "Seleccionar foto" else "Cambiar foto")
            }
            if (photoUrl != null) {
                TextButton(onClick = { onPhotoSelected(null) }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Quitar foto")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUProfilePhotoPickerPreview() {
    RutaUTheme {
        RutaUProfilePhotoPicker(
            fullName = "Mateo Morales",
            photoUrl = null,
            onPhotoSelected = {},
        )
    }
}
