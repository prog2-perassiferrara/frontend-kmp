package com.prog2.frontend.ui.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.prog2.frontend.reservation.data.ReservationProcess

/**
 * Presenta el proceso informado por Turnos: envío en curso, fase, vencimiento,
 * incertidumbre y rechazo. No envía teléfono ni cancela; solo muestra lo recibido.
 */
class ProcessScreen {
    @Composable
    fun Content(
        state: ReservationUiState,
        onRefresh: () -> Unit,
        onVerify: () -> Unit,
        onRetry: () -> Unit,
        onBackToSearch: () -> Unit,
        onBackToAvailability: () -> Unit,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reserva", style = MaterialTheme.typography.h5)
            when {
                state.start == StartStatus.SENDING -> Unit
                state.process == null &&
                    (state.start == StartStatus.REJECTED || state.start == StartStatus.NOT_REGISTERED) ->
                    TextButton(onClick = onBackToAvailability) { Text("Volver a disponibilidad") }
                else -> TextButton(onClick = onBackToSearch) { Text("Volver") }
            }
        }
        if (state.professionalName.isNotBlank()) {
            Text(state.professionalName, style = MaterialTheme.typography.subtitle1)
        }
        val selectionText = listOf(state.date, displayTime(state.startTime))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (selectionText.isNotBlank()) {
            Text(selectionText, style = MaterialTheme.typography.body2)
        }
        Divider()

        val process = state.process
        when {
            process != null -> Process(process, state, onRefresh)
            state.start == StartStatus.SENDING -> Sending()
            state.start == StartStatus.UNCERTAIN -> Uncertain(state, onVerify)
            state.start == StartStatus.NOT_REGISTERED -> NotRegistered(state, onRetry)
            state.start == StartStatus.REJECTED -> Rejected(state.message)
        }
    }

    @Composable
    private fun Sending() {
        CircularProgressIndicator()
        Text("Solicitando la reserva a Turnos…")
        Text(
            "La solicitud se envía una sola vez. Turnos todavía no informó su resultado.",
            style = MaterialTheme.typography.body2,
        )
    }

    @Composable
    private fun Process(process: ReservationProcess, state: ReservationUiState, onRefresh: () -> Unit) {
        val view = processView(process)
        Text(view.title, style = MaterialTheme.typography.subtitle1)
        Text(view.detail)
        processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.body2) }
        process.expiresAt?.let {
            Text(
                "Vencimiento informado por Turnos: ${displayUtc(it)}.",
                style = MaterialTheme.typography.body2,
            )
        }
        if (process.phoneRequired) {
            Notice(
                "Turnos necesita tu número de teléfono para continuar. " +
                    "La carga se habilitará en el próximo paso.",
            )
        }
        state.message?.let { Message(it) }

        if (process.isFinal) {
            Text(
                "El proceso terminó con el resultado informado por Turnos.",
                style = MaterialTheme.typography.body2,
            )
        } else {
            Text(
                "La reserva sigue en curso y todavía no está confirmada.",
                style = MaterialTheme.typography.body2,
            )
            Button(
                onClick = onRefresh,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.loading -> "Actualizando…"
                        state.message != null -> "Reintentar"
                        else -> "Actualizar"
                    },
                )
            }
            if (state.loading) CircularProgressIndicator()
        }
    }

    @Composable
    private fun Uncertain(state: ReservationUiState, onVerify: () -> Unit) {
        Text("No pudimos confirmar si Turnos registró la solicitud.", style = MaterialTheme.typography.subtitle1)
        Text("La solicitud pudo haberse registrado. Verificá el estado antes de volver a iniciarla.")
        state.message?.let { Message(it) }
        Button(
            onClick = onVerify,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.loading) "Verificando…" else "Verificar estado")
        }
        if (state.loading) CircularProgressIndicator()
    }

    @Composable
    private fun NotRegistered(state: ReservationUiState, onRetry: () -> Unit) {
        Text("Turnos no registró el inicio de la reserva.", style = MaterialTheme.typography.subtitle1)
        Text("Podés repetir la misma solicitud; se conserva la misma clave para no duplicarla.")
        state.message?.let { Message(it) }
        Button(
            onClick = onRetry,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.loading) "Reintentando…" else "Reintentar")
        }
        if (state.loading) CircularProgressIndicator()
    }

    @Composable
    private fun Rejected(message: String?) {
        Text(message ?: "Turnos rechazó la solicitud.", style = MaterialTheme.typography.subtitle1)
        Text(
            "No se creó una reserva. Volvé a la disponibilidad para consultar horarios actuales.",
            style = MaterialTheme.typography.body2,
        )
    }

    @Composable
    private fun Notice(message: String) {
        Text(
            message,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    @Composable
    private fun Message(message: String) {
        Text(
            message,
            color = MaterialTheme.colors.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

