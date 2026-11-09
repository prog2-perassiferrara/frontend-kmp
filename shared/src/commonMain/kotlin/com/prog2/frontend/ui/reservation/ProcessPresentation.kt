package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.ui.components.ATTENTION_TIME_ZONE
import com.prog2.frontend.ui.components.displayDate
import com.prog2.frontend.ui.components.formatInstantInZone

/** Texto presentable de una fase informada por el servicio de turnos. */
internal data class ProcessView(val title: String, val detail: String)

/**
 * Agrupa las fases del contrato en textos presentables. No deduce transiciones ni
 * declara vencimientos: solo describe lo que el servicio informó.
 */
internal fun processView(process: ReservationProcess): ProcessView = when (process.state) {
    "HOLD_PENDING", "HELD", "CONFIRMATION_PENDING" -> ProcessView(
        "Preparando la reserva",
        "Estamos reservando el horario. La reserva todavía no está confirmada.",
    )
    "WAITING_FOR_REQUEST" -> ProcessView(
        "Esperando que se solicite el teléfono",
        "Todavía no hace falta completar información. La reserva no está confirmada.",
    )
    "WAITING_FOR_PHONE" -> ProcessView(
        "Falta completar información",
        "Necesitamos un dato adicional para continuar. La reserva todavía no está confirmada.",
    )
    "PHONE_QUEUED" -> ProcessView(
        "Teléfono enviado",
        "Estamos esperando el resultado de la reserva.",
    )
    "WAITING_FOR_RESULT", "RECONCILIATION_REQUIRED" -> ProcessView(
        "Esperando el resultado",
        "Todavía no tenemos un resultado final para esta solicitud.",
    )
    "CONFIRMED" -> ProcessView(
        "Reserva confirmada",
        "Tu turno quedó confirmado.",
    )
    "FAILED" -> ProcessView(
        "Solicitud fallida",
        "No se pudo completar la reserva.",
    )
    "EXPIRED" -> ProcessView(
        "Solicitud vencida",
        "El tiempo para completar la solicitud venció.",
    )
    "CANCELLED" -> ProcessView(
        "Turno cancelado",
        "La reserva fue cancelada.",
    )
    else -> ProcessView(
        "Estado de la solicitud",
        "No podemos mostrar el detalle de este estado. Actualizá para conocer su avance.",
    )
}

/** Motivo adicional del resultado, si el contrato lo informó. */
internal fun processReason(code: String?): String? = when (code) {
    null -> null
    "SLOT_NOT_AVAILABLE" -> "El horario dejó de estar disponible."
    "HOLD_EXPIRED" -> "Se venció el tiempo para completar la reserva."
    "CONFIRMATION_REJECTED" -> "No se pudo confirmar la reserva."
    else -> "No se pudo completar la solicitud por un motivo no detallado."
}

/**
 * Aviso del teléfono cuando el servicio no habilita el formulario. No deduce el
 * motivo: solo distingue un envío ya informado de un envío no disponible.
 */
internal fun phoneNotice(process: ReservationProcess): String? = when {
    process.canSubmitPhone -> null
    process.state in PHONE_SENT_STATES ->
        "El teléfono ya fue enviado. Todavía no recibimos la confirmación de la reserva."
    process.phoneRequired ->
        "No es posible enviar el teléfono nuevamente. Actualizá para conocer el estado."
    else -> null
}

private val PHONE_SENT_STATES =
    setOf("PHONE_QUEUED", "WAITING_FOR_RESULT", "RECONCILIATION_REQUIRED")

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
        process.expiresAt?.let { "La solicitud vence el ${displayExpiration(it)}." }
    }
