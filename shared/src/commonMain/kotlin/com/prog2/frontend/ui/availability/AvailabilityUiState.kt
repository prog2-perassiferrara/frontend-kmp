package com.prog2.frontend.ui.availability

/**
 * Estado temporal de la disponibilidad de un profesional. Se conserva mientras
 * viva el ViewModel; no persiste horarios ni los considera vigentes entre consultas.
 */
data class AvailabilityUiState(
    val professionalId: Long? = null,
    val professionalName: String = "",
    val categoryName: String = "",
    val date: String = "",
    val dateError: String? = null,
    val times: List<String> = emptyList(),
    val selectedTime: String? = null,
    val isLoading: Boolean = false,
    val hasResult: Boolean = false,
    val error: String? = null,
    val retryable: Boolean = false,
) {
    val isEmptyResult: Boolean get() = hasResult && error == null && times.isEmpty()
    val canConsult: Boolean get() = professionalId != null && !isLoading
}
