package com.prog2.frontend.ui.components

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prog2.frontend.navigation.AppDestination

/**
 * Barra de navegación inferior (Material 1 `BottomNavigation`), presentacional:
 * no conoce el `NavController`; solo recibe los destinos, la ruta activa y el
 * callback de selección.
 */
class AppBottomBar {
    /**
     * Dibuja la barra inferior.
     *
     * @param destinations pestañas a mostrar (normalmente `AppDestination.entries`).
     * @param currentRoute ruta activa; determina qué pestaña aparece seleccionada.
     * @param onSelect callback invocado al tocar una pestaña.
     * @param modifier modificador opcional para la barra completa.
     */
    @Composable
    fun Content(
        destinations: List<AppDestination>,
        currentRoute: String?,
        onSelect: (AppDestination) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BottomNavigation(modifier = modifier) {
            destinations.forEach { destination ->
                BottomNavigationItem(
                    selected = currentRoute == destination.route,
                    onClick = { onSelect(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}
