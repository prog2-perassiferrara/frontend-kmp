package com.prog2.frontend.reservation

/**
 * Clasificación mínima de un fallo según lo que Turnos acredita: rechazo conocido,
 * inexistencia del proceso o resultado no verificable.
 */
enum class ReservationFailure {
    REJECTED,
    NOT_FOUND,
    UNCERTAIN,
}

/**
 * Fallo presentable de las operaciones de reserva. `processId` es el proceso propio
 * que Turnos mencionó en el problema, si lo hubo; no acredita éxito por sí solo.
 */
class ReservationException(
    val failure: ReservationFailure,
    val processId: String? = null,
    message: String,
) : Exception(message)
