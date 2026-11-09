package com.prog2.frontend.ui.reservation

import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.reservation.data.ReservationSelection

/** Resultado del inicio según lo que Turnos acreditó; no replica su máquina de estados. */
enum class StartStatus {
    /** Todavía no hay una solicitud propia en curso. */
    IDLE,

    /** El POST está en curso; no se repite. */
    SENDING,

    /** El inicio quedó sin verificar y no se conoce el proceso. */
    UNCERTAIN,

    /** La verificación confirmó que Turnos no registró el inicio. */
    NOT_REGISTERED,

    /** Turnos rechazó el inicio de forma conocida. */
    REJECTED,
}

/** Acción que ofrece el aviso de la búsqueda. */
enum class BannerAction { RETRY_DISCOVERY, SHOW_PROCESS }

/** Aviso no bloqueante sobre un proceso o una solicitud que no se está mostrando. */
data class ReservationBannerState(
    val message: String,
    val actionLabel: String,
    val action: BannerAction,
)

/**
 * Estado de CU-06/CU-08. El progreso mostrado siempre proviene de Turnos; la
 * selección y el `requestId` viven solo mientras dure el ViewModel.
 */
data class ReservationUiState(
    val process: ReservationProcess? = null,
    val start: StartStatus = StartStatus.IDLE,
    val selection: ReservationSelection? = null,
    val requestId: String? = null,
    val loading: Boolean = false,
    val message: String? = null,
) {
    /** El proceso recibido tiene prioridad sobre la selección enviada. */
    val professionalName: String
        get() = process?.professionalName?.takeIf { it.isNotBlank() }
            ?: selection?.professionalName.orEmpty()

    val date: String get() = process?.date ?: selection?.date.orEmpty()

    val startTime: String get() = process?.startTime ?: selection?.startTime.orEmpty()

    /** Aviso que la búsqueda muestra cuando el proceso no está en pantalla. */
    val banner: ReservationBannerState?
        get() = when {
            process != null && !process.isFinal -> ReservationBannerState(
                message = "Tenés un proceso de reserva en curso.",
                actionLabel = "Ver estado",
                action = BannerAction.SHOW_PROCESS,
            )
            start == StartStatus.UNCERTAIN -> ReservationBannerState(
                message = "Hay una solicitud de reserva sin verificar.",
                actionLabel = "Ver estado",
                action = BannerAction.SHOW_PROCESS,
            )
            start == StartStatus.IDLE && message != null -> ReservationBannerState(
                message = message,
                actionLabel = "Reintentar",
                action = BannerAction.RETRY_DISCOVERY,
            )
            else -> null
        }
}
