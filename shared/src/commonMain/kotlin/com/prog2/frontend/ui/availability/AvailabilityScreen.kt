package com.prog2.frontend.ui.availability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prog2.frontend.ui.components.DateSelector
import com.prog2.frontend.ui.components.FieldError
import com.prog2.frontend.ui.components.displayDate

/**
 * Presenta la disponibilidad de CU-05: profesional, fecha, estados y horarios.
 * Permite marcar un horario, pero no reserva ni retiene el horario.
 */
class AvailabilityScreen {
    private val dateSelector = DateSelector()

    @Composable
    fun Content(
        state: AvailabilityUiState,
        onDate: (String) -> Unit,
        onTime: (String) -> Unit,
        onRetry: () -> Unit,
        onReserve: () -> Unit,
        reservationBlocked: String?,
        onBack: () -> Unit,
    ) {
        val enabled = !state.isLoading

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Disponibilidad", style = MaterialTheme.typography.h5)
            TextButton(onClick = onBack) { Text("Volver") }
        }
        Text(state.professionalName, style = MaterialTheme.typography.subtitle1)
        if (state.categoryName.isNotBlank()) {
            Text(state.categoryName, style = MaterialTheme.typography.body2)
        }

        key(state.professionalId) {
            dateSelector.Content(state.date, onDate)
        }
        Divider()
        if (state.isLoading) {
            Text("Consultando…")
            CircularProgressIndicator()
        }

        state.error?.let {
            FieldError(it)
            if (state.retryable) Button(onClick = onRetry, enabled = enabled) { Text("Reintentar") }
        }

        if (state.hasResult) {
            if (state.isEmptyResult) {
                Text("No hay horarios para el ${displayDate(state.date)}.")
            } else {
                Text("Horarios disponibles", style = MaterialTheme.typography.subtitle1)
                Text(displayDate(state.date), style = MaterialTheme.typography.body2)
                state.times.forEach { time ->
                    TimeRow(
                        time = time,
                        selected = state.selectedTime == time,
                        onClick = { onTime(time) },
                    )
                    Divider()
                }
                Button(
                    onClick = onReserve,
                    enabled = state.selectedTime != null && !state.isLoading && reservationBlocked == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Solicitar reserva")
                }
                reservationBlocked?.let {
                    Text(it, style = MaterialTheme.typography.body2)
                }
                Text(
                    "El horario se asegura al confirmar la reserva.",
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }

    @Composable
    private fun TimeRow(time: String, selected: Boolean, onClick: () -> Unit) {
        Row(
            Modifier.fillMaxWidth()
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(displayTime(time), style = MaterialTheme.typography.body1)
        }
    }

    /** El contrato acepta `HH:mm` y `HH:mm:ss`; la pantalla muestra la hora local de atención. */
    private fun displayTime(value: String): String = value.take(5)
}
