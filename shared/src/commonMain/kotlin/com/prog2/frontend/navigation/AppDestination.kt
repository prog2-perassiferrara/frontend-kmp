package com.prog2.frontend.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Punto único de verdad de las pestañas navegables desde la BottomBar.
 *
 * Cada entrada describe una ruta, su etiqueta visible y el icono de la pestaña.
 * Añadir una pantalla nueva requiere una entrada aquí y su `composable(...)` en
 * [MainScreen]; la barra se genera a partir de `AppDestination.entries`.
 *
 * @property route identificador de navegación. Debe ser único entre destinos.
 * @property label etiqueta visible de la pestaña.
 * @property icon vector mostrado como icono de la pestaña.
 */
enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    /** Pestaña de búsqueda de profesionales y acceso a la reserva (CU-04/05). */
    Search(route = "search", label = "Buscar", icon = Icons.Filled.Search),

    /** Pestaña de cuenta: reservas propias y cierre de sesión (CU-09/10). */
    Reservations(route = "reservations", label = "Cuenta", icon = Icons.Filled.Person),
}
