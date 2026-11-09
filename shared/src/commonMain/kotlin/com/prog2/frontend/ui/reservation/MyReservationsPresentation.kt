package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.ui.components.displayDate

/** Texto presentable de las reservas consultadas; no deduce estados no informados. */
internal fun reservationStateLabel(state: String): String = when (state) {
    "CONFIRMED" -> "Confirmada"
    "CANCELLED" -> "Cancelada"
    "FAILED" -> "Fallida"
    "EXPIRED" -> "Vencida"
    else -> "Pendiente"
}

/** Si no se conservó el nombre histórico, se identifica el registro sin inventarlo. */
internal fun reservationProfessional(process: ReservationProcess): String =
    process.professionalName.ifBlank { "Profesional sin nombre registrado" }

/** Fecha y hora local de atención informadas por el servicio. */
internal fun reservationSchedule(process: ReservationProcess): String =
    "${displayDate(process.date)} · ${displayTime(process.startTime)}"

/** Progreso de cancelación informado; un pendiente no acredita el cierre. */
internal fun reservationCancellationNotice(process: ReservationProcess): String? = when {
    process.cancellationPending -> "Cancelación en curso. Actualizá para conocer el resultado."
    process.cancellationRetryRequired -> "La cancelación quedó pendiente de reintento."
    process.state == "CANCELLED" -> "Tu turno fue cancelado."
    else -> null
}
