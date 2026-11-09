package com.prog2.frontend.ui.search

import com.prog2.frontend.catalog.data.CatalogCategory
import com.prog2.frontend.catalog.data.Professional

/**
 * Estado temporal de búsqueda; se conserva mientras viva el ViewModel del proceso.
 * No persiste datos del catálogo ni del usuario.
 */
data class SearchUiState(
    val name: String = "",
    val categoryId: Long? = null,
    val enabled: Boolean? = null,
    val availableOn: String = "",
    val categories: List<CatalogCategory> = emptyList(),
    val categoriesUnavailable: Boolean = false,
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val results: List<Professional> = emptyList(),
    val selected: Professional? = null,
    val isSearching: Boolean = false,
    val hasResult: Boolean = false,
    val paginationStale: Boolean = false,
    val catalogOutdated: Boolean = false,
    val error: String? = null,
    val retryable: Boolean = false,
    val nameError: String? = null,
    val availableOnError: String? = null,
) {
    // Los totales pertenecen a la última consulta: si se editó un filtro, paginar
    // los mezclaría con filtros que Catálogo todavía no evaluó.
    val canGoBack: Boolean get() = hasResult && !paginationStale && page > 0
    val canGoForward: Boolean get() = hasResult && !paginationStale && page + 1 < totalPages
    val isEmptyResult: Boolean get() = hasResult && error == null && results.isEmpty()
}
