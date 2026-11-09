package com.prog2.frontend.availability

/**
 * Fallos presentables de la consulta de disponibilidad; no conservan cuerpos HTTP
 * ni detalles internos de Turnos.
 */
enum class AvailabilityError(val message: String, val retryable: Boolean) {
    INVALID_QUERY("Revisá la fecha: Turnos no acepta esa consulta. Usá AAAA-MM-DD desde hoy.", false),
    PROFESSIONAL_UNAVAILABLE("El profesional no está habilitado para consultar horarios. Volvé a la búsqueda.", false),
    NOT_FOUND("El profesional ya no existe en el catálogo. Volvé a la búsqueda.", false),
    UNAVAILABLE("No se pudo obtener la disponibilidad. Podés volver a intentar.", true),
}

/** Propaga un fallo conocido de disponibilidad sin conservar la respuesta original. */
class AvailabilityException(val error: AvailabilityError) : Exception(error.message)
