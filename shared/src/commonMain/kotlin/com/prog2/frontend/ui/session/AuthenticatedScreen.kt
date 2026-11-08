package com.prog2.frontend.ui.session

import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

/** Área transitoria de sesión, hasta integrar los destinos de los demás CU. */
class AuthenticatedScreen {
    @Composable
    fun Content(onSignOut: () -> Unit) {
        Text("Sesión iniciada", style = MaterialTheme.typography.h5)
        Button(onClick = onSignOut) { Text("Cerrar sesión") }
    }
}
