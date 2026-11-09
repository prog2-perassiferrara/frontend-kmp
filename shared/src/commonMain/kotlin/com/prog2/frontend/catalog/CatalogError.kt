package com.prog2.frontend.catalog

/** Fallos presentables de la búsqueda; no conservan cuerpos HTTP ni detalles internos. */
enum class CatalogError(val message: String) {
    INVALID_CRITERIA("Revisá los filtros: hay un valor con formato inválido."),
    COPY_UNAVAILABLE("La información de profesionales todavía no está disponible. Podés volver a intentar."),
    UNAVAILABLE("No pudimos obtener la información de profesionales. Podés volver a intentar."),
}

/** Propaga un fallo conocido de Catálogo sin conservar la respuesta original. */
class CatalogException(val error: CatalogError) : Exception(error.message)
