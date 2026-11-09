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
    private var discovered = false

    /**
     * Inicia una solicitud nueva desde la selección de CU-05. Solo se acepta desde
     * `IDLE` y sin proceso conocido, para no duplicar el POST.
     */
    fun start(selection: ReservationSelection) {
        val current = mutableState.value
        if (current.start != StartStatus.IDLE || current.process != null) return
        val requestId = newRequestId()
        mutableState.value = ReservationUiState(
            start = StartStatus.SENDING,
            selection = selection,
            requestId = requestId,
        )
        submit(selection, requestId)
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

    /** Consulta por identificador una solicitud cuyo resultado no se pudo verificar. */
    fun verify() {
        val current = mutableState.value
        if (current.start != StartStatus.UNCERTAIN || current.loading) return
        val requestId = current.requestId ?: return
        load(requestId, missingIsUnregistered = true)
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
                onPhoneFailure(process.id, failure)
            } catch (_: SessionException) {
                // App resuelve el cierre o cambio de sesión; no se publica estado ajeno.
            }
        }
    }

    /**
     * Verifica con `GET` un envío incierto. Solo una lectura exitosa habilita
     * volver a enviar; una repetición nunca se hace antes de conocer el estado.
     */
    fun verifyPhone() {
        val current = mutableState.value
        if (current.phone != PhoneStatus.UNCERTAIN || current.loading) return
        val process = current.process ?: return
        load(process.id, missingIsUnregistered = false)
    }

    /** Actualiza manualmente un proceso conocido, sin crear efectos. */
    fun refresh() {
        val current = mutableState.value
        val process = current.process ?: return
        if (current.loading) return
        load(process.id, missingIsUnregistered = false)
    }

    /**
     * Muestra un proceso ya informado por Turnos, por ejemplo desde Mis reservas,
     * sin repetir la lectura. Cancela una operación anterior para no publicar tarde.
     */
    fun present(process: ReservationProcess) {
        job?.cancel()
        job = null
        mutableState.value = ReservationUiState(process = process)
    }

    /**
     * Consulta una sola vez el pendiente propio al entrar. La guarda evita repetir
     * la consulta al rotar o al recomponer la pantalla.
     */
    fun discover() {
        if (discovered) return
        discovered = true
        loadPending()
    }

    /** Repite el descubrimiento pedido desde el aviso no bloqueante. */
    fun retryDiscovery() {
        val current = mutableState.value
        if (current.process != null || current.requestId != null) return
        loadPending()
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
        discovered = false
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
        val processId = failure.processId
        if (processId == null) {
            mutableState.value = mutableState.value.copy(
                start = if (failure.failure == ReservationFailure.UNCERTAIN) {
                    StartStatus.UNCERTAIN
                } else {
                    StartStatus.REJECTED
                },
                message = failure.message,
                loading = false,
            )
            return
        }
        // El error nombró un proceso propio: se consulta antes de mostrar u operar.
        mutableState.value = mutableState.value.copy(
            requestId = processId,
            message = failure.message,
            loading = true,
        )
        try {
            show(reservations.process(processId), failure.message)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (loadFailure: ReservationException) {
            // Sin el GET no se puede afirmar un rechazo: queda verificable.
            mutableState.value = mutableState.value.copy(
                start = StartStatus.UNCERTAIN,
                message = loadFailure.message,
                loading = false,
            )
        } catch (_: SessionException) {
            // Igual que en el POST: el cierre lo resuelve App.
        }
    }

    private fun load(processId: String, missingIsUnregistered: Boolean) {
        job?.cancel()
        mutableState.value = mutableState.value.copy(loading = true, message = null)
        job = viewModelScope.launch {
            try {
                show(reservations.process(processId))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ReservationException) {
                val current = mutableState.value
                mutableState.value = when {
                    failure.failure == ReservationFailure.NOT_FOUND && missingIsUnregistered ->
                        current.copy(start = StartStatus.NOT_REGISTERED, loading = false, message = null)
                    current.process != null ->
                        current.copy(loading = false, message = failure.message)
                    else ->
                        current.copy(start = StartStatus.UNCERTAIN, loading = false, message = failure.message)
                }
            } catch (_: SessionException) {
                // Sin sesión vigente, App vuelve al login y descarta este estado.
            }
        }
    }

    /**
     * Un rechazo conocido se contrasta con `GET`: el proceso pudo vencer o
     * cambiar de fase. Un 404 o un fallo de esa lectura no acreditan que el
     * envío no exista: quedan sin verificar y bloquean otro POST.
     */
    private suspend fun onPhoneFailure(processId: String, failure: ReservationException) {
        if (failure.failure != ReservationFailure.REJECTED) {
            mutableState.value = mutableState.value.copy(
                loading = false,
                phone = PhoneStatus.UNCERTAIN,
                phoneMessage = failure.message,
            )
            return
        }
        mutableState.value = mutableState.value.copy(loading = true)
        try {
            show(reservations.process(processId), phoneMessage = failure.message)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (loadFailure: ReservationException) {
            mutableState.value = mutableState.value.copy(
                loading = false,
                phone = PhoneStatus.UNCERTAIN,
                phoneMessage = failure.message,
            )
        } catch (_: SessionException) {
            // Igual que en el resto de las operaciones: el cierre lo resuelve App.
        }
    }

    private fun loadPending() {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val pending = reservations.pending()
                val current = mutableState.value
                when {
                    pending == null ->
                        mutableState.value = current.copy(message = null)
                    current.process != null || current.requestId != null -> Unit
                    else -> show(pending)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ReservationException) {
                mutableState.value = mutableState.value.copy(message = failure.message)
            } catch (_: SessionException) {
                // Sin sesión vigente, App vuelve al login y descarta este estado.
            }
        }
    }

    private fun show(
        process: ReservationProcess,
        message: String? = null,
        phoneMessage: String? = null,
    ) {
        mutableState.value = mutableState.value.copy(
            process = process,
            start = StartStatus.IDLE,
            requestId = process.id,
            loading = false,
            message = message,
            phone = PhoneStatus.IDLE,
            phoneMessage = phoneMessage,
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
