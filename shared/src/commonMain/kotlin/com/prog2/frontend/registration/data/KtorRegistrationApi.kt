package com.prog2.frontend.registration.data

import com.prog2.frontend.network.ApiConfiguration
import com.prog2.frontend.network.postPublicJson
import com.prog2.frontend.network.readProblem
import com.prog2.frontend.network.withApiFallback
import com.prog2.frontend.registration.RegistrationError
import com.prog2.frontend.registration.RegistrationException
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

/**
 * Consume solo el alta pública de Turnos. Interpreta por estado y `code`; el
 * detalle del servidor nunca se presenta ni se conserva.
 */
class KtorRegistrationApi(
    private val client: HttpClient,
    private val configuration: ApiConfiguration,
) : RegistrationApi {
    override suspend fun register(account: NewAccount) {
        val response = withApiFallback({ throw RegistrationException(RegistrationError.UNCERTAIN) }) {
            client.postPublicJson(
                configuration,
                "/api/register",
                RegistrationRequest(
                    account.login, account.password, account.firstName,
                    account.lastName, account.email, account.langKey,
                ),
            )
        }
        if (response.status.value != 201) throw failureFor(response)
    }

    private suspend fun failureFor(response: HttpResponse): RegistrationException {
        if (response.status.value == 400) {
            val problem = response.readProblem()
            if (problem?.code == "USER_IDENTIFIER_ALREADY_USED") {
                return RegistrationException(RegistrationError.IDENTIFIER_ALREADY_USED)
            }
            if (problem == null || problem.code == null || problem.code == "VALIDATION_ERROR") {
                return RegistrationException(RegistrationError.INVALID_INPUT, problem?.invalidFields().orEmpty())
            }
        }
        // 5xx, redirecciones y estados no previstos pueden ocurrir después de
        // confirmar la escritura: el resultado no es verificable desde el cliente.
        return RegistrationException(RegistrationError.UNCERTAIN)
    }
}

@Serializable
private class RegistrationRequest(
    val login: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val langKey: String,
) {
    override fun toString(): String = "RegistrationRequest(REDACTED)"
}
