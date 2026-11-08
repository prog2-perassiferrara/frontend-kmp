package com.prog2.frontend.ui.search

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog2.frontend.catalog.CatalogError
import com.prog2.frontend.catalog.CatalogException
import com.prog2.frontend.catalog.data.CatalogApi
import com.prog2.frontend.catalog.data.ProfessionalQuery
import com.prog2.frontend.session.SessionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Coordina los filtros, la paginación y las consultas de CU-04 sobre la copia de
 * Catálogo. No ordena ni completa resultados: presenta lo que devuelve el backend.
 */
class SearchViewModel(private val catalogApi: CatalogApi) : ViewModel() {
    private val mutableState = mutableStateOf(SearchUiState())
    val state: State<SearchUiState> = mutableState

    private var loaded = false
    private var searchJob: Job? = null
    private var categoriesJob: Job? = null

    /** Carga inicial única: sin filtros, página 0 y tamaño predeterminado. */
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        loadCategories()
        perform(0)
    }

    fun changeName(value: String) = edit { it.copy(name = value, nameError = null) }

    fun changeCategory(categoryId: Long?) = edit { it.copy(categoryId = categoryId) }

    fun changeEnabled(enabled: Boolean?) = edit { it.copy(enabled = enabled) }

    fun changeAvailableOn(value: String) = edit { it.copy(availableOn = value, availableOnError = null) }

    /** Valida los filtros locales y consulta desde la primera página. */
    fun search() {
        val current = state.value
        if (current.isSearching) return
        val nameError = if (current.name.trim().length > MAX_NAME_LENGTH) {
            "El nombre admite hasta $MAX_NAME_LENGTH caracteres."
        } else {
            null
        }
        val date = current.availableOn.trim()
        val dateError = if (date.isNotEmpty() && !DATE_PATTERN.matches(date)) {
            "Usá el formato AAAA-MM-DD."
        } else {
            null
        }
        updateState { it.copy(nameError = nameError, availableOnError = dateError) }
        if (nameError != null || dateError != null) return
        if (current.categoriesUnavailable) loadCategories()
        perform(0)
    }

    /** Repite la última consulta, incluidas sus categorías si habían fallado. */
    fun retry() {
        if (state.value.isSearching) return
        if (state.value.categoriesUnavailable) loadCategories()
        perform(state.value.page)
    }

    fun nextPage() {
        val current = state.value
        if (!current.isSearching && current.canGoForward) perform(current.page + 1)
    }

    fun previousPage() {
        val current = state.value
        if (!current.isSearching && current.canGoBack) perform(current.page - 1)
    }

    /** Descarta filtros, resultados y consultas pendientes al cerrar la sesión. */
    fun reset() {
        searchJob?.cancel()
        categoriesJob?.cancel()
        mutableState.value = SearchUiState()
        loaded = false
    }

    private fun perform(page: Int) {
        val current = state.value
        if (current.isSearching) return
        updateState {
            it.copy(
                page = page,
                isSearching = true,
                paginationStale = false,
                error = null,
                retryable = false,
                results = emptyList(),
                hasResult = false,
            )
        }
        val query = ProfessionalQuery(
            categoryId = current.categoryId,
            name = current.name.trim().ifEmpty { null },
            enabled = current.enabled,
            availableOn = current.availableOn.trim().ifEmpty { null },
            page = page,
        )
        searchJob = viewModelScope.launch {
            try {
                val result = catalogApi.searchProfessionals(query)
                updateState {
                    it.copy(
                        results = result.content,
                        page = result.page,
                        totalPages = result.totalPages,
                        totalElements = result.totalElements,
                        catalogOutdated = result.catalog.state != "CURRENT" || !result.catalog.copyAvailable,
                        hasResult = true,
                        isSearching = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: CatalogException) {
                // Un filtro rechazado se corrige y se vuelve a buscar; repetirlo no ayuda.
                showFailure(failure.error.message, retryable = failure.error != CatalogError.INVALID_CRITERIA)
            } catch (failure: SessionException) {
                // El cliente protegido ya retiró la sesión si correspondía.
                showFailure(failure.error.message, retryable = true)
            }
        }
    }

    private fun loadCategories() {
        categoriesJob = viewModelScope.launch {
            try {
                val categories = catalogApi.categories()
                updateState { it.copy(categories = categories, categoriesUnavailable = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: CatalogException) {
                updateState { it.copy(categoriesUnavailable = true) }
            } catch (_: SessionException) {
                updateState { it.copy(categoriesUnavailable = true) }
            }
        }
    }

    private fun showFailure(message: String, retryable: Boolean) {
        updateState { it.copy(isSearching = false, hasResult = false, error = message, retryable = retryable) }
    }

    private fun edit(transform: (SearchUiState) -> SearchUiState) {
        if (state.value.isSearching) return
        updateState { transform(it).copy(paginationStale = true) }
    }

    // Las acciones y viewModelScope se ejecutan en Main; la edición se refleja sin
    // un salto de colección adicional entre el IME y el estado consumido por Compose.
    private fun updateState(transform: (SearchUiState) -> SearchUiState) {
        mutableState.value = transform(mutableState.value)
    }

    private companion object {
        const val MAX_NAME_LENGTH = 100

        // Solo verifica la forma ISO que espera el contrato; la fecha real la valida Catálogo.
        val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
