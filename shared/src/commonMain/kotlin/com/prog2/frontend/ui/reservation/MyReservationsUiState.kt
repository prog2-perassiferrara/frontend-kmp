package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess

/**
 * Estado de CU-09/CU-10: página recibida, mensajes y la única operación de
 * cancelación en curso. Las reservas mostradas siempre provienen de Turnos.
 */
data class MyReservationsUiState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val pending: ReservationProcess? = null,
    val reservations: List<ReservationProcess> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val message: String? = null,
    val cancelTarget: ReservationProcess? = null,
    val cancellingId: String? = null,
    val cancelMessage: String? = null,
    val cancelBlockedId: String? = null,
) {
    val canGoBack: Boolean get() = page > 0 && !loading && cancellingId == null

    val canGoForward: Boolean get() = page + 1 < totalPages && !loading && cancellingId == null

    /** Distingue una lista realmente vacía de una consulta todavía no completada. */
    val noHistory: Boolean get() = loaded && message == null && reservations.isEmpty()

    /**
     * Solo Turnos habilita la cancelación. Una operación en curso, una
     * cancelación `PENDING` o un resultado sin verificar bloquean otro envío.
     */
    fun canCancel(process: ReservationProcess): Boolean =
        !loading &&
            cancellingId == null &&
            cancelBlockedId != process.id &&
            process.canRequestCancellation
}
