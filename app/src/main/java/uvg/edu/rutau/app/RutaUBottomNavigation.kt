package uvg.edu.rutau.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uvg.edu.rutau.core.designsystem.component.RutaUBottomBar
import uvg.edu.rutau.core.designsystem.component.RutaUBottomNavigationItem
import uvg.edu.rutau.ui.theme.RutaUTheme

/** App-level bottom navigation for the three authenticated destinations. */
@Composable
fun RutaUBottomNavigation(
    selectedDestination: RutaUTopLevelDestination,
    onDestinationSelected: (RutaUTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    RutaUBottomBar(
        items = RutaUTopLevelDestination.entries.map { destination ->
            destination.toNavigationItem()
        },
        selectedItemId = selectedDestination.name,
        onItemSelected = { item ->
            RutaUTopLevelDestination.entries
                .firstOrNull { it.name == item.id }
                ?.let(onDestinationSelected)
        },
        modifier = modifier,
    )
}

private fun RutaUTopLevelDestination.toNavigationItem(): RutaUBottomNavigationItem = when (this) {
    RutaUTopLevelDestination.TRIPS -> RutaUBottomNavigationItem(
        id = name,
        label = "Trayectos",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
    )
    RutaUTopLevelDestination.REQUESTS -> RutaUBottomNavigationItem(
        id = name,
        label = "Solicitudes",
        icon = Icons.AutoMirrored.Outlined.List,
        selectedIcon = Icons.AutoMirrored.Filled.List,
    )
    RutaUTopLevelDestination.ACCOUNT -> RutaUBottomNavigationItem(
        id = name,
        label = "Cuenta",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person,
    )
}

@Preview(showBackground = true)
@Composable
private fun RutaUBottomNavigationPreview() {
    RutaUTheme {
        RutaUBottomNavigation(
            selectedDestination = RutaUTopLevelDestination.TRIPS,
            onDestinationSelected = {},
        )
    }
}
