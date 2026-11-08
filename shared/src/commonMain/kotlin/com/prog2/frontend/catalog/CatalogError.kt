package com.prog2.frontend.catalog

/** Fallos presentables de la búsqueda; no conservan cuerpos HTTP ni detalles internos. */
enum class CatalogError(val message: String) {
    INVALID_CRITERIA("Revisá los filtros: hay un valor con formato inválido."),
    COPY_UNAVAILABLE("El catálogo todavía no tiene una copia disponible para consultar."),
    UNAVAILABLE("No se pudo consultar el catálogo. Podés volver a intentar."),
}

/** Propaga un fallo conocido de Catálogo sin conservar la respuesta original. */
class CatalogException(val error: CatalogError) : Exception(error.message)
