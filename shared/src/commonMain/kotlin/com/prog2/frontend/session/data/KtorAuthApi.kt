package com.prog2.frontend.session.data

import com.prog2.frontend.network.ApiConfiguration
import com.prog2.frontend.network.postPublicJson
import com.prog2.frontend.network.readProblem
import com.prog2.frontend.network.withApiFallback
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Consume solo la autenticación pública, sin bearer, cookies ni reautenticación automática. */
class KtorAuthApi(
    private val client: HttpClient,
    private val configuration: ApiConfiguration,
) : AuthApi {
    override suspend fun authenticate(username: String, password: String, rememberMe: Boolean): String {
        val response = withApiFallback({ throw SessionException(SessionError.UNAVAILABLE) }) {
            client.postPublicJson(configuration, "/api/authenticate", AuthenticationRequest(username, password, rememberMe))
        }
        if (response.status.value != 200) {
            throw SessionException(errorFor(response.status.value, response.readProblem()?.code))
        }
        return withApiFallback({ throw SessionException(SessionError.INVALID_RESPONSE) }) {
            response.body<AuthenticationResponse>().token.also { require(it.isNotBlank()) }
        }
    }

    private fun errorFor(status: Int, code: String?): SessionError = when {
        status == 400 && (code == null || code == "VALIDATION_ERROR") -> SessionError.INVALID_INPUT
        status == 401 && (code == null || code == "INVALID_CREDENTIALS") -> SessionError.INVALID_CREDENTIALS
        else -> SessionError.UNAVAILABLE
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
