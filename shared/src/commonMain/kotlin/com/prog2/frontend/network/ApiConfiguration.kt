package com.prog2.frontend.network

import io.ktor.http.URLProtocol
import io.ktor.http.Url

/** Orígenes propios autorizados para recibir el JWT, establecidos al iniciar la app. */
class ApiConfiguration(turnosBaseUrl: String, catalogBaseUrl: String, allowLocalHttp: Boolean = false) {
    private val turnos = validate(turnosBaseUrl, allowLocalHttp)
    private val catalog = validate(catalogBaseUrl, allowLocalHttp)

    internal fun baseUrl(backend: Backend): Url = when (backend) {
        Backend.TURNOS -> turnos
        Backend.CATALOG -> catalog
    }

    private fun validate(value: String, allowLocalHttp: Boolean): Url {
        val url = try {
            Url(value)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("La URL del backend no es válida.")
        }
        require(
            url.host.isNotBlank() && url.user == null && url.password == null &&
                url.parameters.isEmpty() && url.fragment.isEmpty() && url.encodedPath in setOf("", "/") &&
                (url.protocol == URLProtocol.HTTPS ||
                    (allowLocalHttp && url.protocol == URLProtocol.HTTP && url.host == "10.0.2.2")),
        ) { "La URL del backend debe ser un origen HTTPS o el destino local de debug permitido." }
        return url
    }
}

enum class Backend { TURNOS, CATALOG }
