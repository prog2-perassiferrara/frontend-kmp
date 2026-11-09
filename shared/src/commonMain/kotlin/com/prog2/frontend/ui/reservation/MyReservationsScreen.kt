package com.prog2.frontend.ui.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
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
        onSignOut: () -> Unit,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mis reservas", style = MaterialTheme.typography.h5)
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
            Divider()
        }

        if (state.noHistory) {
            Text("No hay reservas en el historial.", style = MaterialTheme.typography.body2)
        }
        state.reservations.forEach { reservation ->
            ReservationRow(reservation, state, onOpenCancellation)
            Divider()
        }
        Pagination(state, onPrevious, onNext)

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
        Text("Solicitud de turno pendiente", style = MaterialTheme.typography.subtitle1)
        Text("Todavía no es un turno confirmado.", style = MaterialTheme.typography.body2)
        Text(reservationProfessional(process), style = MaterialTheme.typography.body1)
        Text(reservationSchedule(process), style = MaterialTheme.typography.body2)
        Text("Estado: ${reservationStateLabel(process.state)}", style = MaterialTheme.typography.body2)
        processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.body2) }
        TextButton(onClick = { onOpenProcess(process) }) { Text("Ver estado") }
    }

    @Composable
    private fun ReservationRow(
        process: ReservationProcess,
        state: MyReservationsUiState,
        onOpenCancellation: (ReservationProcess) -> Unit,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(reservationProfessional(process), style = MaterialTheme.typography.subtitle1)
            Text(reservationSchedule(process), style = MaterialTheme.typography.body2)
            Text("Estado: ${reservationStateLabel(process.state)}", style = MaterialTheme.typography.body2)
            processReason(process.reasonCode)?.let { Text(it, style = MaterialTheme.typography.body2) }
            reservationCancellationNotice(process)?.let { Text(it, style = MaterialTheme.typography.body2) }
            when {
                state.cancellingId == process.id -> {
                    Text("Cancelando…", style = MaterialTheme.typography.body2)
                    CircularProgressIndicator()
                }
                state.canCancel(process) -> TextButton(onClick = { onOpenCancellation(process) }) {
                    Text(
                        if (process.cancellationRetryRequired) {
                            "Reintentar cancelación"
                        } else {
                            "Cancelar reserva"
                        },
                    )
                }
                state.cancelBlockedId == process.id ->
                    Text(
                        "Verificá el estado antes de volver a cancelar.",
                        style = MaterialTheme.typography.body2,
                    )
            }
        }
    }

    @Composable
    private fun Pagination(
        state: MyReservationsUiState,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
    ) {
        if (state.totalPages <= 0) return
        Text(
            "Página ${state.page + 1} de ${state.totalPages} · ${state.totalElements} " +
                "${if (state.totalElements == 1L) "reserva" else "reservas"}",
            style = MaterialTheme.typography.body2,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPrevious, enabled = state.canGoBack) { Text("Anterior") }
            TextButton(onClick = onNext, enabled = state.canGoForward) { Text("Siguiente") }
        }
    }

    @Composable
    private fun Notice(message: String, error: Boolean = false) {
        Text(
            message,
            color = if (error) MaterialTheme.colors.error else MaterialTheme.colors.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
