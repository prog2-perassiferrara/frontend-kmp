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

    override suspend fun reservations(page: Int, size: Int): OwnReservationsPage {
        val response = call(LIST_UNCERTAIN) {
            api.get(
                Backend.TURNOS,
                "/api/reservations",
                mapOf("page" to page.toString(), "size" to size.toString()),
            )
        }
        if (response.status != 200) throw uncertain(LIST_UNCERTAIN)
        return decode(LIST_UNCERTAIN) {
            json.decodeFromString<OwnReservationsResponse>(response.body).toPage()
        }
    }

    /**
     * El POST no lleva cuerpo. Un `200` informa un cierre o un reintento requerido;
     * un `202` una cancelación todavía pendiente. Ninguno se interpreta sin decodificar.
     */
    override suspend fun cancel(processId: String): ReservationProcess {
        if (!PROCESS_ID.matches(processId)) throw notFound()
        val response = call(CANCEL_UNCERTAIN) {
            api.post(Backend.TURNOS, "/api/reservation-processes/$processId/cancel")
        }
        if (response.status != 200 && response.status != 202) throw cancelFailure(response)
        return decode(CANCEL_UNCERTAIN) {
            json.decodeFromString<ProcessResponse>(response.body).toProcess()
        }
    }

    override suspend fun submitPhone(processId: String, request: SubmitPhone): ReservationProcess {
        if (!PROCESS_ID.matches(processId)) throw notFound()
        val response = call(PHONE_UNCERTAIN) {
            api.post(
                Backend.TURNOS,
                "/api/reservation-processes/$processId/phone",
                json.encodeToString(PhoneBody(request.requestId, request.phoneNumber)),
            )
        }
        if (response.status != 200 && response.status != 202) throw phoneFailure(response)
        // Un 200/202 ilegible no demuestra que el envío no se registró.
        return decode(PHONE_UNCERTAIN) {
            json.decodeFromString<ProcessResponse>(response.body).toProcess()
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
            "ACTIVE_RESERVATION_PROCESS" -> rejected("Ya tenés una solicitud de turno pendiente.", processId)
            "REQUEST_ID_CONFLICT" -> rejected("La solicitud no coincide con la ya registrada.", processId)
            "SLOT_NOT_AVAILABLE" -> rejected("El horario dejó de estar disponible.", processId)
            "HOLD_EXPIRED" -> rejected("Se venció el tiempo para completar la reserva.", processId)
            "CONFIRMATION_REJECTED" -> rejected("No se pudo confirmar la reserva.", processId)
            "ACCESS_DENIED" -> rejected("No tenés permiso para iniciar esta reserva.", processId)
            else -> when {
                response.status in 500..599 -> uncertain(START_UNCERTAIN, processId)
                response.status == 403 -> rejected("No tenés permiso para iniciar esta reserva.", processId)
                response.status in 400..499 -> rejected("No pudimos iniciar la reserva.", processId)
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

    /**
     * Un `code` conocido del envío es un rechazo presentable. El estado del
     * proceso se contrasta después con `GET`; un `503` o `5xx` no acredita fallo.
     */
    private fun phoneFailure(response: ApiResponse): ReservationException {
        val problem = problem(response.body)
        val processId = problem?.processId
        return when (problem?.code) {
            "INVALID_PHONE_NUMBER" ->
                rejected("El teléfono no tiene un formato válido.", processId)
            "PHONE_NOT_REQUESTED" ->
                rejected("No hace falta enviar el teléfono para esta solicitud.", processId)
            "PHONE_SUBMISSION_ACTIVE" ->
                rejected("Ya hay un envío de teléfono en curso.", processId)
            "PHONE_WINDOW_CLOSED" ->
                rejected("El plazo para enviar el teléfono venció.", processId)
            "REQUEST_ID_CONFLICT" ->
                rejected("El envío no coincide con el ya registrado.", processId)
            "RESERVATION_PROCESS_NOT_FOUND" -> notFound()
            else -> when {
                response.status == 404 -> notFound()
                response.status in 500..599 -> uncertain(PHONE_UNCERTAIN, processId)
                response.status == 403 ->
                    rejected("No tenés permiso para enviar el teléfono.", processId)
                response.status in 400..499 ->
                    rejected("No pudimos registrar el teléfono.", processId)
                else -> uncertain(PHONE_UNCERTAIN, processId)
            }
        }
    }

    /**
     * Solo el rechazo conocido o la inexistencia se presentan como definitivos.
     * Un `503`, `5xx` o respuesta sin código no acredita que la cancelación no se registró.
     */
    private fun cancelFailure(response: ApiResponse): ReservationException {
        val code = problem(response.body)?.code
        return when {
            response.status == 404 || code == "RESERVATION_PROCESS_NOT_FOUND" -> notFound()
            response.status == 409 || code == "CANCELLATION_NOT_ALLOWED" ->
                rejected("Esta reserva no se puede cancelar.")
            response.status == 403 ->
                rejected("No tenés permiso para cancelar esta reserva.")
            response.status in 500..599 -> uncertain(CANCEL_UNCERTAIN)
            response.status in 400..499 -> rejected("No pudimos cancelar la reserva.")
            else -> uncertain(CANCEL_UNCERTAIN)
        }
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

    private fun rejected(message: String, processId: String? = null) =
        ReservationException(ReservationFailure.REJECTED, processId, message)

    private fun notFound() = ReservationException(
        ReservationFailure.NOT_FOUND,
        message = "No encontramos esta solicitud.",
    )

    private fun uncertain(message: String, processId: String? = null) =
        ReservationException(ReservationFailure.UNCERTAIN, processId, message)

    private companion object {
        const val START_UNCERTAIN =
            "No pudimos confirmar si tu solicitud quedó registrada. Pudo haberse registrado."
        const val READ_UNCERTAIN = "No pudimos actualizar el estado de tu solicitud."
        const val LIST_UNCERTAIN = "No pudimos consultar tus reservas."
        const val PHONE_UNCERTAIN =
            "No pudimos confirmar si recibimos tu teléfono. El envío pudo haberse registrado."
        const val CANCEL_UNCERTAIN =
            "No pudimos confirmar si la cancelación se completó. " +
                "Consultá el estado de la reserva antes de reintentarlo."

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
private class PhoneBody(val requestId: String, val phoneNumber: String)

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
    val cancellation: CancellationResponse? = null,
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
        cancellation = cancellation?.let { CancellationProgress(it.status, it.canRetry) },
    )
}

@Serializable
private class CancellationResponse(val status: String, val canRetry: Boolean = false)

@Serializable
private class OwnReservationsResponse(
    val pending: ProcessResponse? = null,
    val history: HistoryResponse? = null,
) {
    /** `pending` puede faltar; el historial completo es obligatorio en una respuesta `200`. */
    fun toPage(): OwnReservationsPage {
        val history = history
            ?: throw IllegalStateException("La respuesta no incluyó el historial.")
        return OwnReservationsPage(
            pending = pending?.toProcess(),
            content = history.content.map { it.toProcess() },
            page = history.page,
            size = history.size,
            totalElements = history.totalElements,
            totalPages = history.totalPages,
        )
    }
}

/** Campos exigidos por el contrato; una página incompleta no equivale a un historial vacío. */
@Serializable
private class HistoryResponse(
    val content: List<ProcessResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Serializable
private class ProblemResponse(val code: String? = null, val processId: String? = null)
