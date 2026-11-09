package com.prog2.frontend.availability.data

import com.prog2.frontend.availability.AvailabilityError
import com.prog2.frontend.availability.AvailabilityException
import com.prog2.frontend.network.ApiResponse
import com.prog2.frontend.network.AuthenticatedApiClient
import com.prog2.frontend.network.Backend
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Consume la consulta de disponibilidad de Turnos con el cliente autenticado.
 * El 401 lo resuelve ese cliente; aquí solo se interpretan los demás estados.
 */
class KtorAvailabilityApi(private val api: AuthenticatedApiClient) : AvailabilityApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun availability(professionalId: Long, date: String): ProfessionalAvailability {
        val response = api.get(
            Backend.TURNOS,
            "/api/appointment-availability",
            mapOf("professionalId" to professionalId.toString(), "date" to date),
        )
        if (response.status != 200) throw failureFor(response)
        return decode { json.decodeFromString<AvailabilityResponse>(response.body).toAvailability() }
    }

    private fun failureFor(response: ApiResponse): AvailabilityException {
        val code = problemCode(response.body)
        return when {
            response.status == 400 && code == "PROFESSIONAL_DISABLED" ->
                AvailabilityException(AvailabilityError.PROFESSIONAL_UNAVAILABLE)
            response.status == 400 -> AvailabilityException(AvailabilityError.INVALID_QUERY)
            response.status == 404 -> AvailabilityException(AvailabilityError.NOT_FOUND)
            else -> AvailabilityException(AvailabilityError.UNAVAILABLE)
        }
    }

    private fun problemCode(body: String): String? = try {
        json.decodeFromString<AvailabilityProblem>(body).code
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    // Una respuesta declarada 200 pero ilegible no demuestra ausencia de horarios.
    private inline fun <T> decode(block: () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        throw AvailabilityException(AvailabilityError.UNAVAILABLE)
    }
}

@Serializable
private class AvailabilityResponse(
    val professionalId: Long,
    val date: String,
    val timeZone: String,
    val availableStartTimes: List<String>,
) {
    fun toAvailability() = ProfessionalAvailability(professionalId, date, timeZone, availableStartTimes)
}

@Serializable
private class AvailabilityProblem(val code: String? = null)
