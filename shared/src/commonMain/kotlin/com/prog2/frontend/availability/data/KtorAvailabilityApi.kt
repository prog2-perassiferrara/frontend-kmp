package com.prog2.frontend.availability.data

import com.prog2.frontend.availability.AvailabilityError
import com.prog2.frontend.availability.AvailabilityException
import com.prog2.frontend.network.ApiResponse
import com.prog2.frontend.network.apiJson
import com.prog2.frontend.network.withApiFallback
import com.prog2.frontend.network.AuthenticatedApiClient
import com.prog2.frontend.network.Backend

/**
 * Consume la consulta de disponibilidad de Turnos con el cliente autenticado.
 * El 401 lo resuelve ese cliente; aquí solo se interpretan los demás estados.
 */
class KtorAvailabilityApi(private val api: AuthenticatedApiClient) : AvailabilityApi {
    override suspend fun availability(professionalId: Long, date: String): ProfessionalAvailability {
        val response = api.get(
            Backend.TURNOS,
            "/api/appointment-availability",
            mapOf("professionalId" to professionalId.toString(), "date" to date),
        )
        if (response.status != 200) throw failureFor(response)
        return withApiFallback({ throw AvailabilityException(AvailabilityError.UNAVAILABLE) }) {
            apiJson.decodeFromString<ProfessionalAvailability>(response.body)
        }
    }

    private fun failureFor(response: ApiResponse): AvailabilityException {
        val code = response.problem?.code
        return when {
            response.status == 400 && code == "PROFESSIONAL_DISABLED" ->
                AvailabilityException(AvailabilityError.PROFESSIONAL_UNAVAILABLE)
            response.status == 400 -> AvailabilityException(AvailabilityError.INVALID_QUERY)
            response.status == 404 -> AvailabilityException(AvailabilityError.NOT_FOUND)
            else -> AvailabilityException(AvailabilityError.UNAVAILABLE)
        }
    }
}
