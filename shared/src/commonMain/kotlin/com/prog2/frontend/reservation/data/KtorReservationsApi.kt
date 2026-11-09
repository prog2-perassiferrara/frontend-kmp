package com.prog2.frontend.reservation.data

import com.prog2.frontend.network.ApiResponse
import com.prog2.frontend.network.AuthenticatedApiClient
import com.prog2.frontend.network.Backend
import com.prog2.frontend.reservation.ReservationException
import com.prog2.frontend.reservation.ReservationFailure
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Consume los contratos de reservas propias de Turnos con el cliente autenticado.
 * Solo interpreta HTTP, `code` y `processId`; el 401 lo resuelve ese cliente y no
 * se reproduce la máquina de estados del backend.
 */
class KtorReservationsApi(private val api: AuthenticatedApiClient) : ReservationsApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun start(request: StartReservation): ReservationProcess {
        val response = call(START_UNCERTAIN) {
            api.post(
                Backend.TURNOS,
                "/api/reservation-processes",
                json.encodeToString(
                    StartBody(
                        request.requestId,
                        request.professionalId,
                        request.date,
                        request.startTime,
                    ),
                ),
            )
        }
        if (response.status != 200 && response.status != 202) throw startFailure(response)
        // Un 200/202 ilegible no demuestra que el proceso no exista.
        return decode(START_UNCERTAIN) {
            json.decodeFromString<ProcessResponse>(response.body).toProcess()
        }
    }

    override suspend fun process(processId: String): ReservationProcess {
        if (!PROCESS_ID.matches(processId)) throw notFound()
        val response = call(READ_UNCERTAIN) {
            api.get(Backend.TURNOS, "/api/reservation-processes/$processId")
        }
        if (response.status != 200) throw processFailure(response)
        return decode(READ_UNCERTAIN) {
            json.decodeFromString<ProcessResponse>(response.body).toProcess()
        }
    }

    override suspend fun pending(): ReservationProcess? {
        val response = call(READ_UNCERTAIN) {
            api.get(Backend.TURNOS, "/api/reservations", mapOf("page" to "0", "size" to "1"))
        }
        if (response.status != 200) throw uncertain(READ_UNCERTAIN)
        return decode(READ_UNCERTAIN) {
            json.decodeFromString<OwnReservationsResponse>(response.body).pending?.toProcess()
        }
    }

    /**
     * Un `code` conocido es un rechazo presentable. El `processId` del problema se
     * conserva para consultar ese proceso; no se interpreta como éxito.
     */
    private fun startFailure(response: ApiResponse): ReservationException {
        val problem = problem(response.body)
        val processId = problem?.processId
        return when (problem?.code) {
            "INVALID_RESERVATION_SELECTION" -> rejected("La selección dejó de ser válida.", processId)
            "PROFILE_NOT_RESERVABLE" -> rejected("El perfil no permite iniciar reservas.", processId)
            "ACTIVE_RESERVATION_PROCESS" -> rejected("Ya tenés un proceso de reserva activo.", processId)
            "REQUEST_ID_CONFLICT" -> rejected("La solicitud no coincide con el inicio registrado.", processId)
            "SLOT_NOT_AVAILABLE" -> rejected("El horario dejó de estar disponible.", processId)
            "HOLD_EXPIRED" -> rejected("El hold venció antes de confirmar la reserva.", processId)
            "CONFIRMATION_REJECTED" -> rejected("La cátedra rechazó la confirmación inicial.", processId)
            "ACCESS_DENIED" -> rejected("No tenés permiso para iniciar esta reserva.", processId)
            else -> when {
                response.status in 500..599 -> uncertain(START_UNCERTAIN, processId)
                response.status == 403 -> rejected("No tenés permiso para iniciar esta reserva.", processId)
                response.status in 400..499 -> rejected("Turnos rechazó la solicitud.", processId)
                else -> uncertain(START_UNCERTAIN, processId)
            }
        }
    }

    /** Solo el 404 o su código acreditan que el proceso no existe; el resto es incierto. */
    private fun processFailure(response: ApiResponse): ReservationException =
        if (response.status == 404 ||
            problem(response.body)?.code == "RESERVATION_PROCESS_NOT_FOUND"
        ) {
            notFound()
        } else {
            uncertain(READ_UNCERTAIN)
        }

    private fun problem(body: String): ProblemResponse? = try {
        json.decodeFromString<ProblemResponse>(body)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    // El transporte no demuestra que la operación no llegó: se informa como incierta.
    private suspend fun <T> call(message: String, block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: SessionException) {
        if (failure.error != SessionError.UNAVAILABLE) throw failure
        throw uncertain(message)
    }

    private inline fun <T> decode(message: String, block: () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        throw uncertain(message)
    }

    private fun rejected(message: String, processId: String?) =
        ReservationException(ReservationFailure.REJECTED, processId, message)

    private fun notFound() = ReservationException(
        ReservationFailure.NOT_FOUND,
        message = "Turnos no registró este proceso.",
    )

    private fun uncertain(message: String, processId: String? = null) =
        ReservationException(ReservationFailure.UNCERTAIN, processId, message)

    private companion object {
        const val START_UNCERTAIN =
            "No pudimos verificar si Turnos registró la solicitud. La solicitud pudo haberse registrado."
        const val READ_UNCERTAIN = "No pudimos consultar el proceso en Turnos."

        val PROCESS_ID = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        )
    }
}

@Serializable
private class StartBody(
    val requestId: String,
    val professionalId: Long,
    val date: String,
    val startTime: String,
)

@Serializable
private class ProcessResponse(
    val processId: String,
    val professionalId: Long,
    val date: String,
    val startTime: String,
    val state: String,
    val expiresAt: String? = null,
    val reasonCode: String? = null,
    val requiredField: String? = null,
    val canSubmitPhone: Boolean = false,
    val professionalFirstName: String? = null,
    val professionalLastName: String? = null,
) {
    fun toProcess() = ReservationProcess(
        id = processId,
        professionalId = professionalId,
        date = date,
        startTime = startTime,
        state = state,
        expiresAt = expiresAt,
        reasonCode = reasonCode,
        requiredField = requiredField,
        canSubmitPhone = canSubmitPhone,
        professionalFirstName = professionalFirstName,
        professionalLastName = professionalLastName,
    )
}

@Serializable
private class OwnReservationsResponse(val pending: ProcessResponse? = null)

@Serializable
private class ProblemResponse(val code: String? = null, val processId: String? = null)
