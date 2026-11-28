package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.ui.components.ATTENTION_TIME_ZONE
import com.prog2.frontend.ui.components.displayDate
import com.prog2.frontend.ui.components.formatInstantInZone

/** Presenta la fase informada por Turnos con un solo texto. */
internal fun processStatusLabel(process: ReservationProcess): String = when (process.state) {
    "HOLD_PENDING", "HELD", "CONFIRMATION_PENDING" -> "Preparando la reserva"
    "WAITING_FOR_REQUEST" -> "Solicitud pendiente"
    "WAITING_FOR_PHONE" -> if (process.canSubmitPhone) "Completá tu teléfono" else "Envío de teléfono no disponible"
    "PHONE_QUEUED" -> "Teléfono enviado"
    "WAITING_FOR_RESULT" -> "Teléfono enviado · esperando el resultado"
    "RECONCILIATION_REQUIRED" -> "Esperando el resultado"
    "CONFIRMED" -> "Reserva confirmada"
    "FAILED" -> "Solicitud fallida"
    "EXPIRED" -> "Solicitud vencida"
    "CANCELLED" -> "Turno cancelado"
    else -> "Estado desconocido. Actualizá para conocer el avance."
}

/** Motivo adicional del resultado, si el contrato lo informó. */
internal fun processReason(code: String?): String? = when (code) {
    null -> null
    "SLOT_NOT_AVAILABLE" -> "El horario dejó de estar disponible."
    "PHONE_REJECTED" -> "Teléfono rechazado."
    "HOLD_EXPIRED" -> "Se venció el tiempo para completar la reserva."
    "CONFIRMATION_REJECTED" -> "No se pudo confirmar la reserva."
    else -> "No se pudo completar la solicitud por un motivo no detallado."
}

/** El contrato acepta `HH:mm` y `HH:mm:ss`; la pantalla muestra la hora local de atención. */
internal fun displayTime(value: String): String = value.take(5)

/**
 * `expiresAt` es un instante UTC; se presenta en la zona de atención con la que la
 * aplicación muestra las fechas de los turnos, sin usar el reloj del dispositivo.
 */
internal fun displayExpiration(value: String): String =
    formatInstantInZone(value, ATTENTION_TIME_ZONE) ?: displayDate(value.substringBefore('T'))

/**
 * Vencimiento presentable solo mientras la solicitud sigue abierta. Un proceso
 * finalizado puede conservar `expiresAt` en el DTO, pero ya no es relevante.
 */
internal fun expirationNotice(process: ReservationProcess): String? =
    if (process.isFinal) {
        null
    } else {
        process.expiresAt?.let { "Vence el ${displayExpiration(it)}." }
    }
