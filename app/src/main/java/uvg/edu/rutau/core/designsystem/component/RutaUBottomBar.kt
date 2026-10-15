package uvg.edu.rutau.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

/** Un destino que puede presentarse en la navegación inferior principal. */
data class RutaUBottomNavigationItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

/** Muestra los botones de la navegación inferior. */
@Composable
fun RutaUBottomBar(
    items: List<RutaUBottomNavigationItem>,
    selectedItemId: String,
    onItemSelected: (RutaUBottomNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEach { item ->
            val selected = item.id == selectedItemId
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                modifier = Modifier.testTag("BottomNavigation-${item.id}"),
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
