package com.prog2.frontend.ui.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.prog2.frontend.reservation.data.ReservationProcess

/**
 * Presenta el proceso informado por Turnos: envío en curso, fase, vencimiento,
 * incertidumbre y rechazo. El teléfono solo se envía si Turnos lo habilita y el
 * resultado se muestra sin declarar una confirmación prematura.
 */
class ProcessScreen {
    @Composable
    fun Content(
        state: ReservationUiState,
        onRefresh: () -> Unit,
        onVerify: () -> Unit,
        onRetry: () -> Unit,
        onPhoneChange: (String) -> Unit,
        onSubmitPhone: () -> Unit,
        onVerifyPhone: () -> Unit,
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
            process != null -> Process(
                process,
                state,
                onRefresh,
                onPhoneChange,
                onSubmitPhone,
                onVerifyPhone,
            )
            state.start == StartStatus.SENDING -> Sending()
            state.start == StartStatus.UNCERTAIN -> Uncertain(state, onVerify)
            state.start == StartStatus.NOT_REGISTERED -> NotRegistered(state, onRetry)
            state.start == StartStatus.REJECTED -> Rejected(state.message)
        }
    }

    @Composable
    private fun Sending() {
        CircularProgressIndicator()
        Text("Enviando tu solicitud…")
        Text(
            "La solicitud se envía una sola vez. Todavía no tenemos un resultado.",
            style = MaterialTheme.typography.body2,
        )
    }

    @Composable
    private fun Process(
        process: ReservationProcess,
        state: ReservationUiState,
        onRefresh: () -> Unit,
        onPhoneChange: (String) -> Unit,
        onSubmitPhone: () -> Unit,
        onVerifyPhone: () -> Unit,
    ) {
        val view = processView(process)
        Text(view.title, style = MaterialTheme.typography.subtitle1)
        Text(view.detail)
        processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.body2) }
        process.expiresAt?.let {
            Text(
                "La solicitud vence el ${displayExpiration(it)}.",
                style = MaterialTheme.typography.body2,
            )
        }
        when {
            state.phone == PhoneStatus.SENDING -> {
                Text("Enviando el teléfono…", style = MaterialTheme.typography.body2)
                CircularProgressIndicator()
            }
            state.phone == PhoneStatus.UNCERTAIN -> {
                Notice(
                    "No pudimos confirmar si recibimos tu teléfono. " +
                        "Verificá el estado antes de volver a enviarlo.",
                )
                Button(
                    onClick = onVerifyPhone,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.loading) "Verificando…" else "Verificar estado")
                }
                if (state.loading) CircularProgressIndicator()
            }
            process.canSubmitPhone -> PhoneForm(state, onPhoneChange, onSubmitPhone)
            else -> phoneNotice(process)?.let { Notice(it) }
        }
        if (state.phone != PhoneStatus.UNCERTAIN) {
            state.phoneMessage?.let { Message(it) }
        }
        state.message?.let { Message(it) }

        if (process.isFinal) {
            Text(
                "La solicitud ya está cerrada.",
                style = MaterialTheme.typography.body2,
            )
        } else {
            Text(
                "Tu solicitud sigue pendiente y todavía no está confirmada.",
                style = MaterialTheme.typography.body2,
            )
            Button(
                onClick = onRefresh,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.phone == PhoneStatus.SENDING -> "Enviando teléfono…"
                        state.loading -> "Actualizando…"
                        state.message != null -> "Reintentar"
                        else -> "Actualizar"
                    },
                )
            }
            if (state.loading && state.phone != PhoneStatus.SENDING) CircularProgressIndicator()
        }
    }

    @Composable
    private fun PhoneForm(
        state: ReservationUiState,
        onPhoneChange: (String) -> Unit,
        onSubmitPhone: () -> Unit,
    ) {
        Text(
            "Necesitamos tu teléfono para continuar.",
            style = MaterialTheme.typography.subtitle2,
        )
        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.loading,
            isError = state.phoneMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (state.canSubmitPhone) onSubmitPhone() },
            ),
        )
        Button(
            onClick = onSubmitPhone,
            enabled = state.canSubmitPhone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enviar teléfono")
        }
        Text(
            "Enviarlo no confirma la reserva: solo lo usamos para esta solicitud.",
            style = MaterialTheme.typography.body2,
        )
    }

    @Composable
    private fun Uncertain(state: ReservationUiState, onVerify: () -> Unit) {
        Text("No pudimos confirmar si tu solicitud quedó registrada.", style = MaterialTheme.typography.subtitle1)
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
        Text("Tu solicitud no quedó registrada.", style = MaterialTheme.typography.subtitle1)
        Text("Podés volver a intentarlo; no se va a duplicar.")
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
        Text(message ?: "No pudimos iniciar la reserva.", style = MaterialTheme.typography.subtitle1)
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

