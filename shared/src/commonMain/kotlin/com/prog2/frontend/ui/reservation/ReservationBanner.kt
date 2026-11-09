package com.prog2.frontend.ui.reservation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Aviso no bloqueante de CU-08 sobre un proceso que no está en pantalla. */
class ReservationBanner {
    @Composable
    fun Content(message: String, actionLabel: String, onAction: () -> Unit) {
        Card(Modifier.fillMaxWidth(), elevation = 0.dp) {
            Column(Modifier.padding(12.dp)) {
                Text(message, style = MaterialTheme.typography.body2)
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
