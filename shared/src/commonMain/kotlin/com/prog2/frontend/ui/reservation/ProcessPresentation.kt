package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess

/** Texto presentable de una fase informada por Turnos. */
internal data class ProcessView(val title: String, val detail: String)

/**
 * Agrupa las fases del contrato en textos presentables. No deduce transiciones ni
 * declara vencimientos: solo describe lo que Turnos informó.
 */
internal fun processView(process: ReservationProcess): ProcessView = when (process.state) {
    "HOLD_PENDING", "HELD", "CONFIRMATION_PENDING" -> ProcessView(
        "Preparando la reserva",
        "Turnos está reteniendo y confirmando el horario. La reserva todavía no está confirmada.",
    )
    "WAITING_FOR_REQUEST", "WAITING_FOR_PHONE", "PHONE_QUEUED" -> ProcessView(
        "Falta completar información",
        "Turnos necesita datos adicionales para continuar. La reserva todavía no está confirmada.",
    )
    "WAITING_FOR_RESULT", "RECONCILIATION_REQUIRED" -> ProcessView(
        "Esperando el resultado",
        "Turnos todavía no informó un resultado final para este proceso.",
    )
    "CONFIRMED" -> ProcessView(
        "Reserva confirmada",
        "Turnos informó que la reserva quedó confirmada.",
    )
    "FAILED", "EXPIRED", "CANCELLED" -> ProcessView(
        "Proceso finalizado",
        "Turnos informó que el proceso terminó.",
    )
    else -> ProcessView(
        "Estado informado por Turnos",
        "Turnos informó el estado ${process.state}. Actualizá para conocer su avance.",
    )
}

/** Motivo adicional del resultado, si el contrato lo informó. */
internal fun processReason(code: String?): String? = when (code) {
    null -> null
    "SLOT_NOT_AVAILABLE" -> "El horario dejó de estar disponible."
    "HOLD_EXPIRED" -> "El hold venció antes de confirmar la reserva."
    "CONFIRMATION_REJECTED" -> "La cátedra rechazó la confirmación inicial."
    else -> "Turnos informó el motivo adicional $code."
}

/** El contrato acepta `HH:mm` y `HH:mm:ss`; la pantalla muestra la hora local de atención. */
internal fun displayTime(value: String): String = value.take(5)

/** `expiresAt` es un instante UTC; se muestra rotulado y sin compararlo con el reloj local. */
internal fun displayUtc(value: String): String =
    value.replace('T', ' ').removeSuffix("Z") + " UTC"
