package com.prog2.frontend.availability.data

import com.prog2.frontend.availability.AvailabilityException

/**
 * Disponibilidad calculada por Turnos para un profesional y una fecha.
 * `startTimes` usa horas locales de atención; una lista vacía es un resultado válido.
 */
data class ProfessionalAvailability(
    val professionalId: Long,
    val date: String,
    val timeZone: String,
    val startTimes: List<String>,
)

/** Consulta de disponibilidad de CU-05; no crea holds, procesos ni reservas. */
interface AvailabilityApi {
    /**
     * Consulta los horarios libres del profesional para una fecha ISO.
     *
     * @throws AvailabilityException con el fallo presentable del contrato.
     */
    suspend fun availability(professionalId: Long, date: String): ProfessionalAvailability
}
