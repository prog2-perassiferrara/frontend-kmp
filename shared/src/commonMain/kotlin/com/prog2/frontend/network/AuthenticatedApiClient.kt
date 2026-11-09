package com.prog2.frontend.network

import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import com.prog2.frontend.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * Frontera de las operaciones protegidas propias: inyecta el bearer, conserva el
 * cuerpo y descarta respuestas de una apertura de sesión anterior.
 */
class AuthenticatedApiClient(
    private val client: HttpClient,
    private val configuration: ApiConfiguration,
    private val sessions: SessionManager,
) {
    /**
     * Consulta una ruta de API propia. Los CU posteriores agregarán sus operaciones
     * cuando se implementen sus contratos.
     *
     * @throws SessionException ante sesión vencida, reemplazada o fallo de comunicación.
     */
    suspend fun get(backend: Backend, path: String, query: Map<String, String> = emptyMap()): ApiResponse {
        require(path.startsWith("/api/")) { "Se requiere una ruta relativa de API." }
        val snapshot = sessions.snapshot()
        val response = try {
            client.get(configuration.baseUrl(backend).toString().trimEnd('/') + path) {
                bearerAuth(snapshot.token)
                url { query.forEach { (key, value) -> parameters.append(key, value) } }
            }.let { ApiResponse(it.status.value, it.bodyAsText()) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            sessions.requireCurrent(snapshot)
            throw SessionException(SessionError.UNAVAILABLE)
        }
        if (response.status == 401) {
            sessions.reject(snapshot)
            throw SessionException(SessionError.SESSION_REQUIRED)
        }
        sessions.requireCurrent(snapshot)
        return response
    }

    /**
     * Envía una operación de API propia con un cuerpo JSON ya serializado. Un fallo
     * de transporte no demuestra que el servidor no haya recibido la solicitud: el
     * consumidor debe tratarlo como resultado incierto cuando la operación tiene efectos.
     *
     * @throws SessionException ante sesión vencida, reemplazada o fallo de comunicación.
     */
    suspend fun post(backend: Backend, path: String, body: String): ApiResponse {
        require(path.startsWith("/api/")) { "Se requiere una ruta relativa de API." }
        val snapshot = sessions.snapshot()
        val response = try {
            client.post(configuration.baseUrl(backend).toString().trimEnd('/') + path) {
                bearerAuth(snapshot.token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.let { ApiResponse(it.status.value, it.bodyAsText()) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            sessions.requireCurrent(snapshot)
            throw SessionException(SessionError.UNAVAILABLE)
        }
        if (response.status == 401) {
            sessions.reject(snapshot)
            throw SessionException(SessionError.SESSION_REQUIRED)
        }
        sessions.requireCurrent(snapshot)
        return response
    }
}

/** Respuesta privada, sin impresión accidental de datos pertenecientes al usuario. */
class ApiResponse(val status: Int, val body: String) {
    override fun toString(): String = "ApiResponse(status=$status, body=REDACTED)"
}
