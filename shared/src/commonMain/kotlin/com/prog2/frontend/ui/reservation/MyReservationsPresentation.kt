package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess

/** Texto presentable de las reservas consultadas; no deduce estados no informados. */
internal fun reservationStateLabel(state: String): String = when (state) {
    "CONFIRMED" -> "Confirmada"
    "CANCELLED" -> "Cancelada"
    "FAILED" -> "Fallida"
    "EXPIRED" -> "Vencida"
    else -> state
}

/** Turnos puede no conservar el nombre histórico; se identifica el registro sin inventarlo. */
internal fun reservationProfessional(process: ReservationProcess): String =
    process.professionalName.ifBlank { "Profesional ${process.professionalId}" }

/** Fecha y hora local de atención informadas por Turnos. */
internal fun reservationSchedule(process: ReservationProcess): String =
    "${process.date} · ${displayTime(process.startTime)}"

/** Progreso de cancelación informado; `PENDING` no acredita el cierre. */
internal fun reservationCancellationNotice(process: ReservationProcess): String? = when {
    process.cancellationPending -> "Cancelación en curso. Actualizá para conocer el resultado."
    process.cancellationRetryRequired -> "La cancelación quedó pendiente de reintento."
    process.state == "CANCELLED" -> "Turnos confirmó la cancelación."
    else -> null
}
