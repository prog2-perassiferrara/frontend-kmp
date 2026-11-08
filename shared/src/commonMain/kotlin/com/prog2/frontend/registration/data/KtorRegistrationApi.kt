package com.prog2.frontend.registration.data

import com.prog2.frontend.network.ApiConfiguration
import com.prog2.frontend.network.Backend
import com.prog2.frontend.registration.RegistrationError
import com.prog2.frontend.registration.RegistrationException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
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
        val response = try {
            client.post(configuration.baseUrl(Backend.TURNOS).toString().trimEnd('/') + "/api/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegistrationRequest(
                        login = account.login,
                        password = account.password,
                        firstName = account.firstName,
                        lastName = account.lastName,
                        email = account.email,
                        langKey = account.langKey,
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Un fallo de transporte o un timeout no demuestra que el alta falló.
            throw RegistrationException(RegistrationError.UNCERTAIN)
        }
        if (response.status.value == 201) return
        throw failureFor(response)
    }

    private suspend fun failureFor(response: HttpResponse): RegistrationException {
        if (response.status.value == 400) {
            val problem = try {
                response.body<RegistrationProblem>()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (problem?.code == "USER_IDENTIFIER_ALREADY_USED") {
                return RegistrationException(RegistrationError.IDENTIFIER_ALREADY_USED)
            }
            if (problem == null || problem.code == null || problem.code == "VALIDATION_ERROR") {
                return RegistrationException(RegistrationError.INVALID_INPUT, problem?.fieldsByField().orEmpty())
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

@Serializable
private class RegistrationProblem(
    val code: String? = null,
    val fieldErrors: List<RegistrationFieldError> = emptyList(),
) {
    fun fieldsByField(): Map<String, String> = fieldErrors.mapNotNull { error ->
        val field = error.field ?: return@mapNotNull null
        field to (error.code ?: "Invalid")
    }.toMap()
}

@Serializable
private class RegistrationFieldError(val field: String? = null, val code: String? = null)
