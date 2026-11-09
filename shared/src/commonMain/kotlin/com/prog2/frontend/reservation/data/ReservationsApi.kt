package com.prog2.frontend.reservation.data

import com.prog2.frontend.reservation.ReservationException

/**
 * Selección vigente de CU-05 que se envía al iniciar la reserva. El nombre se usa
 * solo para presentar el proceso mientras Turnos responde.
 */
data class ReservationSelection(
    val professionalId: Long,
    val date: String,
    val startTime: String,
    val professionalName: String = "",
)

/** Cuerpo idempotente del inicio; `requestId` es el UUID local que Turnos usa como `processId`. */
data class StartReservation(
    val requestId: String,
    val professionalId: Long,
    val date: String,
    val startTime: String,
)

/**
 * Cuerpo del teléfono ya normalizado. Cada envío o corrección usa un `requestId`
 * nuevo; repetir la misma clave y contenido solo recupera el envío anterior.
 */
data class SubmitPhone(
    val requestId: String,
    val phoneNumber: String,
) {
    /** El teléfono es un dato personal; no se incluye en representaciones de texto. */
    override fun toString(): String = "SubmitPhone[requestId=$requestId]"
}

/**
 * Progreso que informa Turnos. `state` es la fase durable recibida y no se
 * interpreta como confirmación por sí sola.
 */
data class ReservationProcess(
    val id: String,
    val professionalId: Long,
    val date: String,
    val startTime: String,
    val state: String,
    val expiresAt: String?,
    val reasonCode: String?,
    val requiredField: String?,
    val canSubmitPhone: Boolean,
    val professionalFirstName: String?,
    val professionalLastName: String?,
    val cancellation: CancellationProgress? = null,
) {
    /** Nombre histórico informado por Turnos, si lo conserva. */
    val professionalName: String
        get() = listOfNotNull(professionalFirstName, professionalLastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** El contrato de Turnos cierra el proceso en estas fases. */
    val isFinal: Boolean get() = state in FINAL_STATES

    /** Turnos pidió teléfono o habilitó un envío; la forma solo se muestra si `canSubmitPhone`. */
    val phoneRequired: Boolean get() = requiredField == PHONE_NUMBER || canSubmitPhone

    /** Solo una reserva confirmada sin cancelación en curso puede solicitar cancelación. */
    val canRequestCancellation: Boolean
        get() = state == CONFIRMED && cancellation?.status != CANCELLATION_PENDING

    val cancellationPending: Boolean get() = cancellation?.status == CANCELLATION_PENDING

    val cancellationRetryRequired: Boolean get() = cancellation?.status == CANCELLATION_RETRY_REQUIRED

    private companion object {
        const val PHONE_NUMBER = "PHONE_NUMBER"
        const val CONFIRMED = "CONFIRMED"
        const val CANCELLATION_PENDING = "PENDING"
        const val CANCELLATION_RETRY_REQUIRED = "RETRY_REQUIRED"
        val FINAL_STATES = setOf("FAILED", "EXPIRED", "CONFIRMED", "CANCELLED")
    }
}

/** Intención de cancelación informada por Turnos; `CANCELLED` es el único cierre. */
data class CancellationProgress(val status: String, val canRetry: Boolean)

/** Pendiente e historial propios en la misma instantánea, con la paginación de Turnos. */
data class OwnReservationsPage(
    val pending: ReservationProcess?,
    val content: List<ReservationProcess>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** Contratos de reserva propios expuestos por Turnos a la app. */
interface ReservationsApi {
    /**
     * Inicia el proceso de reserva. Turnos responde el proceso durable; una
     * repetición propia se acepta con el mismo `requestId` sin crear otro.
     *
     * @throws ReservationException con el fallo presentable del contrato.
     */
    suspend fun start(request: StartReservation): ReservationProcess

    /**
     * Consulta el progreso durable de un proceso propio.
     *
     * @throws ReservationException con `NOT_FOUND` o `UNCERTAIN`.
     */
    suspend fun process(processId: String): ReservationProcess

    /**
     * Devuelve el proceso pendiente propio, si Turnos informa uno.
     *
     * @throws ReservationException si la consulta no puede resolverse.
     */
    suspend fun pending(): ReservationProcess?

    /**
     * Consulta el pendiente y el historial propios en una página.
     *
     * @param page página desde cero, según el contrato.
     * @param size tamaño entre 1 y 100.
     * @throws ReservationException si la consulta no puede resolverse.
     */
    suspend fun reservations(page: Int, size: Int): OwnReservationsPage

    /**
     * Solicita cancelar una reserva propia confirmada. Turnos responde el proceso
     * actualizado; solo `CANCELLED` acredita el cierre.
     *
     * @throws ReservationException con el rechazo, la inexistencia o la
     *   incertidumbre informada por el contrato.
     */
    suspend fun cancel(processId: String): ReservationProcess

    /**
     * Envía el teléfono ya normalizado del proceso propio. Turnos responde el
     * proceso actualizado; el envío no confirma la reserva por sí solo.
     *
     * @throws ReservationException con el rechazo, la inexistencia o la
     *   incertidumbre informada por el contrato.
     */
    suspend fun submitPhone(processId: String, request: SubmitPhone): ReservationProcess
}
