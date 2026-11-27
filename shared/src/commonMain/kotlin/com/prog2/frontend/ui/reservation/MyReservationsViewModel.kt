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

    /** Cancela y consulta solo esa reserva cuando la respuesta no permite actualizarla. */
    fun confirmCancellation() {
        val current = state.value
        val target = current.cancelTarget ?: return
        if (!current.canCancel(target)) return
        mutableState.value = current.copy(cancelTarget = null, cancellingId = target.id, cancelMessage = null)
        job = viewModelScope.launch {
            try {
                showCancellation(reservations.cancel(target.id))
            } catch (failure: ReservationException) {
                val after = state.value
                mutableState.value = after.copy(
                    unverifiedCancellations = after.unverifiedCancellations + target.id,
                    cancelMessage = failure.message,
                )
                readCancellation(target.id)
            } catch (_: SessionException) {
                // App descarta el estado privado al cerrar la sesión.
            }
        }
    }

    /** Verifica con GET una cancelación que quedó sin respuesta comprobable. */
    fun verifyCancellation(id: String) {
        val current = state.value
        if (current.loading || current.cancellingId != null || id !in current.unverifiedCancellations) return
        mutableState.value = current.copy(cancellingId = id)
        job = viewModelScope.launch { readCancellation(id) }
    }

    private suspend fun readCancellation(id: String) {
        try {
            showCancellation(reservations.process(id), state.value.cancelMessage)
        } catch (_: ReservationException) {
            mutableState.value = state.value.copy(cancellingId = null)
        } catch (_: SessionException) {
            mutableState.value = state.value.copy(cancellingId = null)
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
                cancelMessage = current.cancelMessage.takeIf { current.unverifiedCancellations.isNotEmpty() },
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

    private fun showCancellation(updated: ReservationProcess, message: String? = null) {
        val current = mutableState.value
        mutableState.value = current.copy(
            pending = if (current.pending?.id == updated.id) updated else current.pending,
            reservations = current.reservations.map { if (it.id == updated.id) updated else it },
            cancellingId = null,
            unverifiedCancellations = current.unverifiedCancellations - updated.id,
            cancelMessage = message.takeIf { updated.state == "CONFIRMED" && updated.cancellation == null },
        )
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
