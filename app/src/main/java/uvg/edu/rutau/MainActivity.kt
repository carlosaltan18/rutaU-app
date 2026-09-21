package uvg.edu.rutau

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.app.RutaUApp
import uvg.edu.rutau.ui.theme.RutaUTheme

/** Abre la interfaz principal de RutaU. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutaUTheme {
                RutaUApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RutaUAppPreview() {
    RutaUTheme {
        RutaUApp()
    }
}
