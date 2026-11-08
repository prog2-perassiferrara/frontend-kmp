package com.prog2.frontend.session.data

import com.prog2.frontend.network.ApiConfiguration
import com.prog2.frontend.network.Backend
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Consume solo la autenticación pública, sin bearer, cookies ni reautenticación automática. */
class KtorAuthApi(
    private val client: HttpClient,
    private val configuration: ApiConfiguration,
) : AuthApi {
    override suspend fun authenticate(username: String, password: String, rememberMe: Boolean): String {
        val response = try {
            client.post(configuration.baseUrl(Backend.TURNOS).toString().trimEnd('/') + "/api/authenticate") {
                contentType(ContentType.Application.Json)
                setBody(AuthenticationRequest(username, password, rememberMe))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw SessionException(SessionError.UNAVAILABLE)
        }
        if (response.status.value != 200) throw SessionException(errorFor(response))
        return try {
            response.body<AuthenticationResponse>().token.also {
                if (it.isBlank()) throw SessionException(SessionError.INVALID_RESPONSE)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw SessionException(SessionError.INVALID_RESPONSE)
        }
    }

    private suspend fun errorFor(response: io.ktor.client.statement.HttpResponse): SessionError {
        val code = try {
            response.body<AuthenticationProblem>().code
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return when {
            response.status.value == 400 && (code == null || code == "VALIDATION_ERROR") -> SessionError.INVALID_INPUT
            response.status.value == 401 && (code == null || code == "INVALID_CREDENTIALS") -> SessionError.INVALID_CREDENTIALS
            else -> SessionError.UNAVAILABLE
        }
    }
}

@Serializable
private class AuthenticationRequest(val username: String, val password: String, val rememberMe: Boolean) {
    override fun toString(): String = "AuthenticationRequest(REDACTED)"
}

@Serializable
private class AuthenticationResponse(@SerialName("id_token") val token: String) {
    override fun toString(): String = "AuthenticationResponse(REDACTED)"
}

@Serializable
private class AuthenticationProblem(val code: String? = null)
