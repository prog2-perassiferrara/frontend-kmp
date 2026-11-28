package com.prog2.frontend.ui.availability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            Text("Disponibilidad", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Volver") }
        }
        Text(state.professionalName, style = MaterialTheme.typography.titleMedium)
        if (state.categoryName.isNotBlank()) {
            Text(state.categoryName, style = MaterialTheme.typography.bodyMedium)
        }

        key(state.professionalId) {
            dateSelector.Content(state.date, onDate)
        }
        HorizontalDivider()
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
                Text("Horarios disponibles", style = MaterialTheme.typography.titleMedium)
                Text(displayDate(state.date), style = MaterialTheme.typography.bodyMedium)
                state.times.forEach { time ->
                    TimeRow(
                        time = time,
                        selected = state.selectedTime == time,
                        onClick = { onTime(time) },
                    )
                    HorizontalDivider()
                }
                Button(
                    onClick = onReserve,
                    enabled = state.selectedTime != null && !state.isLoading && reservationBlocked == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Solicitar reserva")
                }
                reservationBlocked?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "El horario se asegura al confirmar la reserva.",
                    style = MaterialTheme.typography.bodyMedium,
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
            Text(displayTime(time), style = MaterialTheme.typography.bodyLarge)
        }
    }

    /** El contrato acepta `HH:mm` y `HH:mm:ss`; la pantalla muestra la hora local de atención. */
    private fun displayTime(value: String): String = value.take(5)
}
