package uvg.edu.rutau.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.ui.theme.RutaUSpacing
import uvg.edu.rutau.ui.theme.RutaUTheme

@Preview(showBackground = true)
@Composable
private fun RutaUTopAppBarPreview() {
    RutaUTheme {
        RutaUTopAppBar(
            title = "Crear cuenta",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationIconContentDescription = "Volver",
            onNavigationClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUBottomBarPreview() {
    RutaUTheme {
        RutaUBottomBar(
            items = listOf(
                RutaUBottomNavigationItem("trips", "Trayectos", Icons.Outlined.Home, Icons.Filled.Home),
                RutaUBottomNavigationItem("account", "Cuenta", Icons.Outlined.Person, Icons.Filled.Person),
            ),
            selectedItemId = "trips",
            onItemSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUButtonsPreview() {
    RutaUTheme {
        Column(
            modifier = Modifier.padding(RutaUSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Small),
        ) {
            RutaUPrimaryButton("Acción principal", {}, Modifier.fillMaxWidth())
            RutaUSecondaryButton("Acción secundaria", {}, Modifier.fillMaxWidth())
            RutaUOutlinedButton("Acción alternativa", {}, Modifier.fillMaxWidth())
            RutaUDestructiveButton("Eliminar", {}, Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUFormFieldsPreview() {
    RutaUTheme {
        Column(
            modifier = Modifier.padding(RutaUSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Small),
        ) {
            RutaUTextField("mateo@ejemplo.com", {}, "Correo electrónico", Modifier.fillMaxWidth())
            RutaUPasswordField("RutaU123", {}, "Contraseña", Modifier.fillMaxWidth())
            RutaUDropdownField(
                selectedOption = "Campus Central",
                options = listOf("Campus Central", "Campus Sur"),
                onOptionSelected = {},
                label = "Campus",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUInformationComponentsPreview() {
    RutaUTheme {
        Column(
            modifier = Modifier.padding(RutaUSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(RutaUSpacing.Medium),
        ) {
            RutaUAvatar(fullName = "Mateo Morales Silva", photoUrl = null)
            RutaUStatusChip("Pendiente", RutaUStatusType.WARNING, icon = Icons.Filled.Info)
            RutaUInfoCard(
                title = "Coordinación entre pares",
                message = "La solicitud permanecerá pendiente hasta recibir una respuesta.",
                icon = Icons.Filled.Info,
            )
            RutaUSectionTitle(title = "Datos personales", actionLabel = "Editar", onActionClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUScreenAndLoadingPreview() {
    RutaUTheme {
        RutaUScreenContainer(
            topBar = { RutaUTopAppBar("RutaU") },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(RutaUSpacing.ScreenHorizontal),
            ) {
                Text("Contenido de ejemplo")
                RutaULoadingButton(
                    text = "Guardar",
                    onClick = {},
                    isLoading = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RutaUConfirmationDialogPreview() {
    RutaUTheme {
        RutaUConfirmationDialog(
            title = "¿Cerrar sesión?",
            message = "Tendrás que iniciar sesión nuevamente para continuar.",
            confirmLabel = "Cerrar sesión",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
