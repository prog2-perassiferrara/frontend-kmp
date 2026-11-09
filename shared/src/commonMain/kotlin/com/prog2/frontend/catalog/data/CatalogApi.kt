package com.prog2.frontend.catalog.data

import com.prog2.frontend.catalog.CatalogException

/** Categoría publicada por Catálogo; se usa como opción del filtro y en los resultados. */
data class CatalogCategory(val id: Long, val name: String, val enabled: Boolean)

/** Profesional resumido de una búsqueda. */
data class Professional(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean,
    val category: CatalogCategory,
)

/**
 * Resumen de la copia consultada. Solo `CURRENT` acredita vigencia comprobada;
 * cualquier otro valor se presenta como posible desactualización.
 */
data class CatalogState(val state: String, val copyAvailable: Boolean)

/** Página de resultados y estado de la misma lectura. */
data class ProfessionalPage(
    val content: List<Professional>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val catalog: CatalogState,
)

/** Filtros y paginación de CU-04; los nulos omiten el parámetro. */
data class ProfessionalQuery(
    val categoryId: Long? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
    val availableOn: String? = null,
    val page: Int = 0,
    val size: Int = DEFAULT_SIZE,
) {
    companion object {
        const val DEFAULT_SIZE = 20
    }
}

/** Consultas de Catálogo de CU-04, consumidas con el JWT del usuario. */
interface CatalogApi {
    /**
     * Busca profesionales en la copia local de Catálogo.
     *
     * @throws CatalogException con el fallo presentable del contrato.
     */
    suspend fun searchProfessionals(query: ProfessionalQuery): ProfessionalPage

    /**
     * Obtiene todas las categorías, incluidas las deshabilitadas.
     *
     * @throws CatalogException con el fallo presentable del contrato.
     */
    suspend fun categories(): List<CatalogCategory>
}
