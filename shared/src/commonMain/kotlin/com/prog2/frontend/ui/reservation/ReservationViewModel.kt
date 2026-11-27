package com.prog2.frontend.ui.reservation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog2.frontend.reservation.ReservationException
import com.prog2.frontend.reservation.ReservationFailure
import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.reservation.data.ReservationSelection
import com.prog2.frontend.reservation.data.ReservationsApi
import com.prog2.frontend.reservation.data.StartReservation
import com.prog2.frontend.reservation.data.SubmitPhone
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * Coordina CU-06, CU-07 y CU-08 sobre los contratos de Turnos. Genera una clave
 * por acción, no repite efectos sin verificar y presenta el progreso recibido sin
 * reproducir la máquina de estados del backend.
 */
class ReservationViewModel(
    private val reservations: ReservationsApi,
    private val newRequestId: () -> String = { Uuid.random().toString() },
) : ViewModel() {
    private val mutableState = mutableStateOf(ReservationUiState())
    val state: State<ReservationUiState> = mutableState

    private var job: Job? = null

    /** Inicia una solicitud y devuelve si la pantalla puede abrir su seguimiento. */
    fun start(selection: ReservationSelection): Boolean {
        if (!state.value.canStart) return false
        val requestId = newRequestId()
        mutableState.value = ReservationUiState(
            start = StartStatus.SENDING,
            selection = selection,
            requestId = requestId,
        )
        submit(selection, requestId)
        return true
    }

    /**
     * Repite la misma solicitud solo después de que la verificación confirmó que
     * Turnos no la registró. Conserva `requestId` para mantener la idempotencia.
     */
    fun retryStart() {
        val current = mutableState.value
        if (current.start != StartStatus.NOT_REGISTERED) return
        val selection = current.selection ?: return
        val requestId = current.requestId ?: return
        mutableState.value = current.copy(start = StartStatus.SENDING, message = null)
        submit(selection, requestId)
    }

    /** Actualiza el texto del teléfono mientras no haya un envío o verificación en curso. */
    fun changePhone(value: String) {
        val current = mutableState.value
        if (current.phone != PhoneStatus.IDLE) return
        mutableState.value = current.copy(phoneNumber = value, phoneMessage = null)
    }

    /**
     * Envía una sola vez el teléfono ya normalizado. Solo actúa cuando Turnos
     * habilitó el envío; el `requestId` es nuevo para cada intento.
     */
    fun submitPhone() {
        val current = mutableState.value
        val process = current.process ?: return
        if (!current.canSubmitPhone) return
        val phoneNumber = normalizePhone(current.phoneNumber)
        if (phoneNumber == null) {
            mutableState.value = current.copy(phoneMessage = INVALID_PHONE_MESSAGE)
            return
        }
        val requestId = newRequestId()
        mutableState.value = current.copy(
            phone = PhoneStatus.SENDING,
            phoneMessage = null,
            loading = true,
        )
        job?.cancel()
        job = viewModelScope.launch {
            try {
                show(reservations.submitPhone(process.id, SubmitPhone(requestId, phoneNumber)))
                // El teléfono ya viajó; no se conserva el texto enviado.
                mutableState.value = mutableState.value.copy(phoneNumber = "")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ReservationException) {
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    phone = PhoneStatus.UNCERTAIN,
                    phoneMessage = failure.message,
                )
            } catch (_: SessionException) {
                // App resuelve el cierre o cambio de sesión; no se publica estado ajeno.
            }
        }
    }

    /** Lee el proceso conocido o verifica el UUID del intento de inicio. */
    fun refresh() {
        val current = state.value
        if (current.loading || current.start == StartStatus.SENDING) return
        val id = current.process?.id
            ?: current.requestId?.takeIf { current.start == StartStatus.UNCERTAIN }
            ?: return
        mutableState.value = current.copy(loading = true, message = null)
        job = viewModelScope.launch { readProcess(id, verifyStart = current.process == null) }
    }

    /**
     * Muestra un proceso ya informado por Turnos, por ejemplo desde Mis reservas,
     * sin repetir la lectura. Cancela una operación anterior para no publicar tarde.
     */
    fun present(process: ReservationProcess) {
        val current = state.value
        if (current.process?.id == process.id && (current.loading || current.phone == PhoneStatus.UNCERTAIN)) return
        job?.cancel()
        job = null
        mutableState.value = ReservationUiState(process = process)
    }

    /**
     * Descarta el estado que ya no requiere seguimiento. Un proceso pendiente o una
     * solicitud incierta o en curso se conservan para poder retomarlos.
     */
    fun dismiss() {
        val current = mutableState.value
        val discarded = current.process?.isFinal == true ||
            current.process == null &&
            (current.start == StartStatus.REJECTED || current.start == StartStatus.NOT_REGISTERED)
        if (discarded) reset()
    }

    /** Descarta el estado temporal al cerrar sesión. Cancela la operación en curso. */
    fun reset() {
        job?.cancel()
        job = null
        mutableState.value = ReservationUiState()
    }

    private fun submit(selection: ReservationSelection, requestId: String) {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                show(
                    reservations.start(
                        StartReservation(
                            requestId = requestId,
                            professionalId = selection.professionalId,
                            date = selection.date,
                            startTime = selection.startTime,
                        ),
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ReservationException) {
                onStartFailure(failure)
            } catch (_: SessionException) {
                // App resuelve el cierre o cambio de sesión; no se publica estado ajeno.
            }
        }
    }

    private suspend fun onStartFailure(failure: ReservationException) {
        val current = state.value
        val processId = failure.processId
        mutableState.value = current.copy(
            start = if (failure.failure == ReservationFailure.UNCERTAIN) StartStatus.UNCERTAIN else StartStatus.REJECTED,
            message = failure.message,
            loading = processId != null,
        )
        if (processId != null) readProcess(processId, message = failure.message)
    }

    private suspend fun readProcess(id: String, verifyStart: Boolean = false, message: String? = null) {
        try {
            show(reservations.process(id), message)
        } catch (failure: ReservationException) {
            val current = state.value
            mutableState.value = when {
                current.process != null -> current.copy(loading = false, message = failure.message)
                id != current.requestId -> current.copy(
                    start = StartStatus.REJECTED,
                    loading = false,
                    message = "No pudimos abrir el pendiente anterior. Revisá tus solicitudes en Cuenta.",
                )
                verifyStart && failure.failure == ReservationFailure.NOT_FOUND ->
                    current.copy(start = StartStatus.NOT_REGISTERED, loading = false, message = null)
                else -> current.copy(start = StartStatus.UNCERTAIN, loading = false, message = failure.message)
            }
        } catch (_: SessionException) {
            // App descarta el estado privado cuando deja de estar vigente la sesión.
        }
    }

    private fun show(process: ReservationProcess, message: String? = null) {
        val current = state.value
        mutableState.value = current.copy(
            process = process,
            start = StartStatus.IDLE,
            selection = current.selection.takeIf { process.id == current.requestId },
            loading = false,
            message = message,
            phone = PhoneStatus.IDLE,
            phoneMessage = null,
        )
    }
}

/**
 * Regla local equivalente al contrato: quita los separadores permitidos y exige
 * entre 7 y 15 dígitos, conservando el signo y los ceros iniciales.
 *
 * @return teléfono normalizado o `null` si no cumple el contrato.
 */
internal fun normalizePhone(value: String): String? = value
    .replace(" ", "")
    .replace("-", "")
    .replace("(", "")
    .replace(")", "")
    .replace(".", "")
    .takeIf { PHONE.matches(it) }

private val PHONE = Regex("^\\+?[0-9]{7,15}$")

private const val INVALID_PHONE_MESSAGE =
    "Ingresá un teléfono válido de 7 a 15 dígitos; podés usar +, espacios, guiones, paréntesis o puntos."
