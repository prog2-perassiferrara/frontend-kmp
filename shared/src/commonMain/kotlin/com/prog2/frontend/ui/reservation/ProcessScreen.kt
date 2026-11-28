package com.prog2.frontend.ui.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

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
        onRetry: () -> Unit,
        onPhoneChange: (String) -> Unit,
        onSubmitPhone: () -> Unit,
        onBack: () -> Unit,
        backLabel: String,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reserva", style = MaterialTheme.typography.headlineSmall)
            if (state.start != StartStatus.SENDING) {
                TextButton(onClick = onBack) { Text(backLabel) }
            }
        }
        if (state.professionalName.isNotBlank()) {
            Text(state.professionalName, style = MaterialTheme.typography.titleMedium)
        }
        val selectionText = listOf(state.date, displayTime(state.startTime))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (selectionText.isNotBlank()) {
            Text(selectionText, style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()

        val process = state.process
        if (process != null) {
            val status = when (state.phone) {
                PhoneStatus.SENDING -> "Enviando teléfono…"
                PhoneStatus.UNCERTAIN -> "Actualizá el estado antes de volver a enviar el teléfono."
                PhoneStatus.IDLE -> processStatusLabel(process)
            }
            Text(status, style = MaterialTheme.typography.titleMedium)
            processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            expirationNotice(process)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (state.phone == PhoneStatus.IDLE && process.canSubmitPhone) {
                PhoneForm(state, onPhoneChange, onSubmitPhone)
            }
        } else {
            when (state.start) {
                StartStatus.SENDING -> Text("Enviando tu solicitud…")
                StartStatus.UNCERTAIN -> Text("Actualizá el estado para verificar si tu solicitud quedó registrada.")
                StartStatus.NOT_REGISTERED -> Text("Tu solicitud no quedó registrada.")
                StartStatus.REJECTED, StartStatus.IDLE -> Unit
            }
        }
        if (state.phone != PhoneStatus.IDLE) state.phoneMessage?.let { Message(it) }
        state.message?.let { Message(it) }
        if (state.loading || state.start == StartStatus.SENDING) CircularProgressIndicator()

        if (state.start == StartStatus.NOT_REGISTERED) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Reintentar inicio") }
        } else if (state.start == StartStatus.UNCERTAIN || process?.isFinal == false) {
            Button(onClick = onRefresh, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.loading && state.phone != PhoneStatus.SENDING) "Actualizando…" else "Actualizar estado")
            }
        }
    }

    @Composable
    private fun PhoneForm(
        state: ReservationUiState,
        onPhoneChange: (String) -> Unit,
        onSubmitPhone: () -> Unit,
    ) {
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
        state.phoneMessage?.let { Message(it) }
    }

    @Composable
    private fun Message(message: String) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

