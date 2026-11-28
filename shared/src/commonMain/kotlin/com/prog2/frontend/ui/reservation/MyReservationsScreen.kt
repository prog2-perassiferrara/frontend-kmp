package com.prog2.frontend.ui.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.prog2.frontend.ui.components.PageControls
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prog2.frontend.reservation.data.ReservationProcess

/**
 * Presenta el pendiente diferenciado y el historial de reservas propias con su
 * paginación. La cancelación solo se envía desde el diálogo de confirmación.
 */
class MyReservationsScreen {
    @Composable
    fun Content(
        state: MyReservationsUiState,
        onRefresh: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onOpenProcess: (ReservationProcess) -> Unit,
        onOpenCancellation: (ReservationProcess) -> Unit,
        onDismissCancellation: () -> Unit,
        onConfirmCancellation: () -> Unit,
        onVerifyCancellation: (String) -> Unit,
        onSignOut: () -> Unit,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mis reservas", style = MaterialTheme.typography.headlineSmall)
            Row {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.loading && state.cancellingId == null,
                ) { Text(if (state.loading) "Actualizando…" else "Actualizar") }
                TextButton(onClick = onSignOut) { Text("Cerrar sesión") }
            }
        }

        if (state.loading && !state.loaded) CircularProgressIndicator()
        state.cancelMessage?.let { Notice(it) }
        state.message?.let {
            Notice(it, error = true)
            Button(onClick = onRefresh, enabled = !state.loading) { Text("Reintentar") }
            if (state.loading) CircularProgressIndicator()
        }

        state.pending?.let { pending ->
            PendingProcess(pending, onOpenProcess)
            HorizontalDivider()
        }

        if (state.noHistory) {
            Text("No hay reservas en el historial.", style = MaterialTheme.typography.bodyMedium)
        }
        state.reservations.forEach { reservation ->
            ReservationRow(reservation, state, onOpenCancellation, onVerifyCancellation)
            HorizontalDivider()
        }
        PageControls(
                state.page, state.totalPages, state.totalElements,
                if (state.totalElements == 1L) "reserva" else "reservas",
                state.canGoBack, state.canGoForward, onPrevious, onNext,
            )

        state.cancelTarget?.let { target ->
            AlertDialog(
                onDismissRequest = onDismissCancellation,
                title = { Text("Cancelar reserva") },
                text = {
                    Text(
                        "¿Cancelar la reserva con ${reservationProfessional(target)} del " +
                            "${reservationSchedule(target)}? La cancelación se confirmará cuando se complete.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmCancellation) { Text("Confirmar cancelación") }
                },
                dismissButton = {
                    TextButton(onClick = onDismissCancellation) { Text("Volver") }
                },
            )
        }
    }

    @Composable
    private fun PendingProcess(process: ReservationProcess, onOpenProcess: (ReservationProcess) -> Unit) {
        Text("Solicitud pendiente", style = MaterialTheme.typography.titleMedium)
        Text(
            "${reservationProfessional(process)} · ${reservationSchedule(process)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        TextButton(onClick = { onOpenProcess(process) }) { Text("Ver estado") }
    }

    @Composable
    private fun ReservationRow(
        process: ReservationProcess,
        state: MyReservationsUiState,
        onOpenCancellation: (ReservationProcess) -> Unit,
        onVerifyCancellation: (String) -> Unit,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(reservationProfessional(process), style = MaterialTheme.typography.titleMedium)
            Text(reservationSchedule(process), style = MaterialTheme.typography.bodyMedium)
            Text("Estado: ${reservationStateLabel(process.state)}", style = MaterialTheme.typography.bodyMedium)
            processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            reservationCancellationNotice(process)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            when {
                state.cancellingId == process.id -> {
                    Text("Cancelando…", style = MaterialTheme.typography.bodyMedium)
                    CircularProgressIndicator()
                }
                process.id in state.unverifiedCancellations -> TextButton(
                    onClick = { onVerifyCancellation(process.id) },
                    enabled = !state.loading && state.cancellingId == null,
                ) { Text("Verificar cancelación") }
                state.canCancel(process) -> TextButton(onClick = { onOpenCancellation(process) }) {
                    Text(
                        if (process.cancellationRetryRequired) {
                            "Reintentar cancelación"
                        } else {
                            "Cancelar reserva"
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun Notice(message: String, error: Boolean = false) {
        Text(
            message,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
