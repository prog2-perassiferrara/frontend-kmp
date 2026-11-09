package com.prog2.frontend.ui.availability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prog2.frontend.ui.components.DatePickerModal
import com.prog2.frontend.ui.components.displayDate
import com.prog2.frontend.ui.components.isoDate

/**
 * Presenta la disponibilidad de CU-05: profesional, fecha, estados y horarios.
 * Permite marcar un horario, pero no reserva ni retiene el horario.
 */
class AvailabilityScreen {
    @Composable
    fun Content(
        state: AvailabilityUiState,
        onDate: (String) -> Unit,
        onConsult: () -> Unit,
        onTime: (String) -> Unit,
        onRetry: () -> Unit,
        onReserve: () -> Unit,
        reservationBlocked: String?,
        onBack: () -> Unit,
    ) {
        val enabled = !state.isLoading
        var datePickerVisible by remember { mutableStateOf(false) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Disponibilidad", style = MaterialTheme.typography.h5)
            TextButton(onClick = onBack) { Text("Volver") }
        }
        Text(state.professionalName, style = MaterialTheme.typography.subtitle1)
        if (state.categoryName.isNotBlank()) {
            Text(state.categoryName, style = MaterialTheme.typography.body2)
        }

        Text("Fecha", style = MaterialTheme.typography.body2)
        OutlinedButton(
            onClick = { datePickerVisible = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.date.isBlank()) "Seleccionar fecha" else displayDate(state.date))
        }
        state.dateError?.let { FieldError(it) }

        Button(onClick = onConsult, enabled = state.canConsult, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isLoading) "Consultando…" else "Consultar horarios")
        }
        if (state.isLoading) CircularProgressIndicator()

        state.error?.let {
            FieldError(it)
            if (state.retryable) Button(onClick = onRetry, enabled = enabled) { Text("Reintentar") }
        }

        if (state.hasResult) {
            if (state.isEmptyResult) {
                Text("No hay horarios disponibles para esa fecha.")
            } else {
                Text("Horarios disponibles", style = MaterialTheme.typography.subtitle1)
                state.times.forEach { time ->
                    TimeRow(
                        time = time,
                        selected = state.selectedTime == time,
                        onClick = { onTime(time) },
                    )
                    Divider()
                }
                state.selectedTime?.let {
                    Text(
                        "Horario elegido: ${displayTime(it)}. Todavía no está reservado.",
                        style = MaterialTheme.typography.body2,
                    )
                }
                if (state.selectedTime == null) {
                    Text(
                        "Elegí un horario para solicitar la reserva.",
                        style = MaterialTheme.typography.body2,
                    )
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
                    "Consultar no reserva el horario: puede ocuparse antes de que lo solicites. " +
                        "La solicitud queda pendiente hasta que se confirme.",
                    style = MaterialTheme.typography.body2,
                )
            }
        }

        if (datePickerVisible) {
            DatePickerModal(
                initialDate = state.date,
                onDateSelected = { year, month, day ->
                    datePickerVisible = false
                    onDate(isoDate(year, month, day))
                },
                onDismiss = { datePickerVisible = false },
            )
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

    @Composable
    private fun FieldError(message: String) {
        Text(
            message,
            color = MaterialTheme.colors.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Preview
@Composable
private fun AvailabilityScreenPreview() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AvailabilityScreen().Content(
                state = AvailabilityUiState(
                    professionalId = 201,
                    professionalName = "Ana Torres",
                    categoryName = "Cardiologia",
                    date = "2026-11-09",
                    times = listOf("12:00:00", "12:30:00"),
                    selectedTime = "12:30:00",
                    hasResult = true,
                ),
                onDate = {}, onConsult = {}, onTime = {}, onRetry = {},
                onReserve = {}, reservationBlocked = null, onBack = {},
            )
        }
    }
}
