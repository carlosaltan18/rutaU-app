package uvg.edu.rutau.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import uvg.edu.rutau.core.designsystem.component.RutaUScreenContainer
import uvg.edu.rutau.core.designsystem.component.RutaUTopAppBar
import uvg.edu.rutau.core.navigation.LegalDocumentType
import uvg.edu.rutau.ui.theme.RutaUSpacing

/** Muestra un documento legal con la misma ruta, sin importar desde dónde se abra. */
@Composable
fun LegalDocumentScreen(
    documentType: LegalDocumentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val document = when (documentType) {
        LegalDocumentType.TERMS -> LegalDocument.TERMS
        LegalDocumentType.PRIVACY -> LegalDocument.PRIVACY
    }
    RutaUScreenContainer(
        modifier = modifier,
        topBar = {
            RutaUTopAppBar(
                title = document.title,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "Volver",
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = RutaUSpacing.ScreenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Medium),
        ) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = document.content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
