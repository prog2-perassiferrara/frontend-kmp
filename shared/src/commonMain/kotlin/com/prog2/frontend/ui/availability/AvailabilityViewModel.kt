package com.prog2.frontend.ui.availability

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog2.frontend.availability.AvailabilityException
import com.prog2.frontend.availability.data.AvailabilityApi
import com.prog2.frontend.catalog.data.Professional
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Coordina la consulta de CU-05 sobre Turnos. Presenta los horarios recibidos,
 * permite marcarlos visualmente y no calcula disponibilidad ni reserva.
 */
class AvailabilityViewModel(private val availabilityApi: AvailabilityApi) : ViewModel() {
    private val mutableState = mutableStateOf(AvailabilityUiState())
    val state: State<AvailabilityUiState> = mutableState

    private var requestJob: Job? = null

    /**
     * Fija el profesional elegido y su fecha inicial. No reinicia el estado si el
     * profesional ya es el mismo, de modo que la rotación conserve fecha y horarios.
     */
    fun open(professional: Professional, initialDate: String) {
        if (mutableState.value.professionalId == professional.id) return
        requestJob?.cancel()
        mutableState.value = AvailabilityUiState(
            professionalId = professional.id,
            professionalName = "${professional.firstName} ${professional.lastName}",
            categoryName = professional.category.name,
            date = initialDate.trim().takeIf { DATE_PATTERN.matches(it) }.orEmpty(),
        )
    }

    /**
     * Edita la fecha y descarta los horarios anteriores: pertenecen a otra fecha y
     * Turnos todavía no se pronunció sobre esta. Cancela la consulta en curso.
     */
    fun changeDate(value: String) {
        requestJob?.cancel()
        updateState {
            it.copy(
                date = value,
                dateError = null,
                times = emptyList(),
                selectedTime = null,
                hasResult = false,
                isLoading = false,
                error = null,
                retryable = false,
            )
        }
    }

    /**
     * Marca visualmente un horario de la lista consultada. No crea holds ni
     * procesos: iniciar la reserva pertenece a CU-06.
     */
    fun selectTime(time: String) {
        val current = state.value
        if (!current.hasResult || current.error != null || time !in current.times) return
        updateState { it.copy(selectedTime = time) }
    }

    /** Valida la forma de la fecha y consulta disponibilidad. */
    fun consult() {
        val current = state.value
        if (!current.canConsult) return
        val date = current.date.trim()
        if (!DATE_PATTERN.matches(date)) {
            updateState { it.copy(dateError = "Usá el formato AAAA-MM-DD.") }
            return
        }
        request(current.professionalId ?: return, date)
    }

    /** Repite la consulta vigente tras un fallo reintentable. */
    fun retry() {
        val current = state.value
        if (!current.canConsult || current.error == null) return
        request(current.professionalId ?: return, current.date.trim())
    }

    /** Descarta la selección y la consulta pendiente al volver o cerrar sesión. */
    fun reset() {
        requestJob?.cancel()
        mutableState.value = AvailabilityUiState()
    }

    private fun request(professionalId: Long, date: String) {
        updateState {
            it.copy(
                date = date,
                isLoading = true,
                dateError = null,
                times = emptyList(),
                selectedTime = null,
                hasResult = false,
                error = null,
                retryable = false,
            )
        }
        requestJob = viewModelScope.launch {
            try {
                val availability = availabilityApi.availability(professionalId, date)
                updateState {
                    it.copy(times = availability.startTimes, isLoading = false, hasResult = true)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: AvailabilityException) {
                showFailure(failure.error.message, failure.error.retryable)
            } catch (failure: SessionException) {
                // El cliente protegido ya retiró la sesión si correspondía.
                showFailure(failure.error.message, retryable = true)
            }
        }
    }

    private fun showFailure(message: String, retryable: Boolean) {
        updateState { it.copy(isLoading = false, hasResult = false, error = message, retryable = retryable) }
    }

    private fun updateState(transform: (AvailabilityUiState) -> AvailabilityUiState) {
        mutableState.value = transform(mutableState.value)
    }

    private companion object {
        // Solo verifica la forma ISO del contrato; el rango lo valida Turnos.
        val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
