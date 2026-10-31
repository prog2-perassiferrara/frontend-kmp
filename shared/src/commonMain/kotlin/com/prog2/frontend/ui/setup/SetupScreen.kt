package com.prog2.frontend.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Representa la demo sin conservar ni modificar su estado de presentación. */
class SetupScreen {
    /**
     * Muestra el estado recibido y comunica las pulsaciones al responsable del estado.
     *
     * @param isContentVisible Indica si debe representarse el contenido demostrativo.
     * @param onToggleContent Acción solicitada al pulsar el botón.
     */
    @Composable
    fun Content(isContentVisible: Boolean, onToggleContent: () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("FrontendKmp", style = MaterialTheme.typography.h5)
                Button(onClick = onToggleContent) {
                    Text(if (isContentVisible) "Ocultar contenido" else "Mostrar contenido")
                }
                if (isContentVisible) {
                    Text("Aplicación KMP ejecutándose en Android.")
                }
            }
        }
    }
}

@Preview
@Composable
private fun SetupScreenPreview() {
    MaterialTheme {
        SetupScreen().Content(isContentVisible = true, onToggleContent = {})
    }
}
