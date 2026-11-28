package com.prog2.frontend.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prog2.frontend.navigation.AppDestination

/**
 * Barra de navegación inferior (Material 3 `NavigationBar`), presentacional:
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
        NavigationBar(modifier = modifier, windowInsets = WindowInsets(0, 0, 0, 0)) {
            destinations.forEach { destination ->
                NavigationBarItem(
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
