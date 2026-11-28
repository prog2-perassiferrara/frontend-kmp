package com.prog2.frontend.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

internal val apiJson = Json { ignoreUnknownKeys = true }

@Serializable
internal class ApiProblem(
    val code: String? = null,
    @SerialName("processId") private val process: JsonElement? = null,
    private val fieldErrors: JsonElement? = null,
) {
    // Los detalles opcionales no invalidan el código del problema.
    val processId: String? get() = (process as? JsonPrimitive)?.takeIf { it.isString }?.content

    fun invalidFields(): Set<String> {
        val fields = fieldErrors ?: return emptySet()
        return withApiFallback({ emptySet() }) {
            apiJson.decodeFromJsonElement<List<ApiFieldError>>(fields).mapNotNull { it.field }.toSet()
        }
    }
}

@Serializable
private class ApiFieldError(val field: String? = null)

internal suspend fun HttpResponse.readProblem(): ApiProblem? = withApiFallback({ null }) { body<ApiProblem>() }

/** Envía JSON a una ruta pública de Turnos sin agregar credenciales de sesión. */
internal suspend inline fun <reified T : Any> HttpClient.postPublicJson(
    configuration: ApiConfiguration,
    path: String,
    payload: T,
): HttpResponse = post(configuration.baseUrl(Backend.TURNOS).toString().trimEnd('/') + path) {
    contentType(ContentType.Application.Json)
    setBody(payload)
}

/** Comparte la recuperación ante fallos sin interceptar la cancelación de la corrutina. */
internal inline fun <T> withApiFallback(onFailure: () -> T, block: () -> T): T = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    onFailure()
}
