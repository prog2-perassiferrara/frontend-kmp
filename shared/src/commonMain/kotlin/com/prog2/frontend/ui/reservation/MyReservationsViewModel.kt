package com.prog2.frontend.ui.reservation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog2.frontend.reservation.ReservationException
import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.reservation.data.ReservationsApi
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Consulta y cancela reservas propias (CU-09/CU-10). La página y los estados
 * provienen de Turnos; solo se guarda el progreso de la operación en curso y el
 * bloqueo de una reserva cuyo último resultado no pudo verificarse.
 */
class MyReservationsViewModel(
    private val reservations: ReservationsApi,
) : ViewModel() {
    private val mutableState = mutableStateOf(MyReservationsUiState())
    val state: State<MyReservationsUiState> = mutableState

    private var job: Job? = null

    /** Consulta la primera página cada vez que se abre la pantalla. */
    fun load() {
        fetch(0)
    }

    /** Actualiza la página actual sin cambiar de selección. */
    fun refresh() {
        fetch(state.value.page)
    }

    fun previous() {
        val current = state.value
        if (current.canGoBack) fetch(current.page - 1)
    }

    fun next() {
        val current = state.value
        if (current.canGoForward) fetch(current.page + 1)
    }

    /** Abre el diálogo solo para una reserva que Turnos permite cancelar ahora. */
    fun openCancellation(process: ReservationProcess) {
        val current = state.value
        if (!current.canCancel(process)) return
        mutableState.value = current.copy(cancelTarget = process, cancelMessage = null)
    }

    /** Cerrar el diálogo no envía nada ni altera la reserva. */
    fun dismissCancellation() {
        if (mutableState.value.cancellingId != null) return
        mutableState.value = mutableState.value.copy(cancelTarget = null)
    }

    /**
     * Envía una sola cancelación y aplica el progreso informado. Ante un rechazo
     * o un resultado incierto, consulta el listado antes de habilitar otro intento.
     */
    fun confirmCancellation() {
        val current = state.value
        val target = current.cancelTarget ?: return
        if (current.cancellingId != null) return
        mutableState.value = current.copy(
            cancelTarget = null,
            cancellingId = target.id,
            cancelMessage = null,
        )
        job?.cancel()
        job = viewModelScope.launch {
            try {
                showCancellation(reservations.cancel(target.id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ReservationException) {
                val page = mutableState.value.page
                mutableState.value = mutableState.value.copy(
                    cancellingId = null,
                    cancelMessage = failure.message,
                    // Un rechazo también puede dejar el estado anterior a la vista:
                    // se exige una lectura exitosa antes de habilitar otro intento.
                    cancelBlockedId = target.id,
                )
                // Una lectura exitosa reconcilia el estado y vuelve a habilitar la acción.
                try {
                    read(page)
                } catch (_: SessionException) {
                    // App cierra la sesión y descarta el bloqueo; mientras tanto no
                    // se habilita otro intento con la fila anterior sin verificar.
                }
            } catch (_: SessionException) {
                // App cierra la sesión y descarta este estado.
            }
        }
    }

    /** Descarta estados y bloqueos al cerrar sesión; la próxima apertura vuelve a consultar. */
    fun reset() {
        job?.cancel()
        job = null
        mutableState.value = MyReservationsUiState()
    }

    private fun fetch(page: Int) {
        val current = mutableState.value
        if (current.loading || current.cancellingId != null) return
        job?.cancel()
        job = viewModelScope.launch {
            try {
                read(page)
            } catch (_: SessionException) {
                // Sin sesión vigente, App vuelve al login y descarta este estado.
            }
        }
    }

    private suspend fun read(page: Int) {
        mutableState.value = mutableState.value.copy(loading = true, message = null)
        try {
            val result = reservations.reservations(page, PAGE_SIZE)
            val current = mutableState.value
            mutableState.value = current.copy(
                loading = false,
                loaded = true,
                message = null,
                pending = result.pending,
                reservations = result.content,
                page = result.page,
                totalPages = result.totalPages,
                totalElements = result.totalElements,
                cancelBlockedId = null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: ReservationException) {
            mutableState.value = mutableState.value.copy(
                loading = false,
                loaded = true,
                message = failure.message,
            )
        }
    }

    private fun showCancellation(updated: ReservationProcess) {
        val current = mutableState.value
        mutableState.value = current.copy(
            pending = if (current.pending?.id == updated.id) updated else current.pending,
            reservations = current.reservations.map { if (it.id == updated.id) updated else it },
            cancellingId = null,
            cancelBlockedId = null,
            cancelMessage = when {
                updated.state == "CANCELLED" -> "Tu turno fue cancelado."
                updated.cancellationPending ->
                    "Tu cancelación está en curso."
                updated.cancellationRetryRequired ->
                    "No pudimos confirmar la cancelación; podés reintentar."
                else -> "La reserva cambió de estado; actualizá para confirmarlo."
            },
        )
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
