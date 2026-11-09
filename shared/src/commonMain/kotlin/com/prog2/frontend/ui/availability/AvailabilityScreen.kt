package com.prog2.frontend.ui.availability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Presenta la disponibilidad de CU-05: profesional, fecha, estados y horarios de
 * Turnos. Permite marcar un horario, pero no reserva ni crea holds.
 */
class AvailabilityScreen {
    @Composable
    fun Content(
        state: AvailabilityUiState,
        onDate: (String) -> Unit,
        onConsult: () -> Unit,
        onTime: (String) -> Unit,
        onRetry: () -> Unit,
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

        OutlinedTextField(
            value = state.date,
            onValueChange = onDate,
            label = { Text("Fecha (AAAA-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.dateError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (state.canConsult) onConsult() }),
        )
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
                Text(
                    "Consultar no reserva el horario: puede ocuparse antes de reservarlo. " +
                        "Elegir un horario solo lo marca; iniciar la reserva pertenece al paso siguiente.",
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
                onDate = {}, onConsult = {}, onTime = {}, onRetry = {}, onBack = {},
            )
        }
    }
}
