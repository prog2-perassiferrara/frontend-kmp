package com.prog2.frontend.network

import com.prog2.frontend.registration.RegistrationError
import com.prog2.frontend.registration.RegistrationException
import com.prog2.frontend.registration.data.KtorRegistrationApi
import com.prog2.frontend.registration.data.NewAccount
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class KtorRegistrationApiTest {
    private val configuration = ApiConfiguration("https://turnos.test", "https://catalog.test")
    private val account = NewAccount(
        login = "patient",
        password = "  🔐secret  ",
        firstName = "Ana",
        lastName = "Pérez",
        email = "patient@example.test",
    )

    @Test
    fun postsExactRegistrationWithoutBearerAndConfirmsCreated() = runTest {
        var requests = 0
        val client = ApiHttpClient().create(MockEngine { request ->
            requests++
            assertEquals("/api/register", request.url.encodedPath)
            assertEquals("turnos.test", request.url.host)
            assertNull(request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals("patient", body["login"]!!.jsonPrimitive.content)
            assertEquals("  🔐secret  ", body["password"]!!.jsonPrimitive.content)
            assertEquals("Ana", body["firstName"]!!.jsonPrimitive.content)
            assertEquals("Pérez", body["lastName"]!!.jsonPrimitive.content)
            assertEquals("patient@example.test", body["email"]!!.jsonPrimitive.content)
            assertEquals("es", body["langKey"]!!.jsonPrimitive.content)
            assertNull(body["imageUrl"])
            respond("", HttpStatusCode.Created)
        })
        try {
            KtorRegistrationApi(client, configuration).register(account)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun mapsValidationFieldsAndDuplicateWithoutRetrying() = runTest {
        val validation = """{"code":"VALIDATION_ERROR","fieldErrors":[""" +
            """{"field":"login","code":"Pattern"},{"field":"email","code":"Email"}]}"""
        val cases = listOf(
            Triple(400, validation, RegistrationError.INVALID_INPUT),
            Triple(400, """{"code":"VALIDATION_ERROR"}""", RegistrationError.INVALID_INPUT),
            Triple(400, "no-json", RegistrationError.INVALID_INPUT),
            Triple(400, """{"code":"USER_IDENTIFIER_ALREADY_USED"}""", RegistrationError.IDENTIFIER_ALREADY_USED),
            Triple(503, """{"code":"USER_REGISTRATION_UNAVAILABLE"}""", RegistrationError.UNCERTAIN),
            Triple(500, """{"code":"INTERNAL_ERROR"}""", RegistrationError.UNCERTAIN),
            Triple(302, "", RegistrationError.UNCERTAIN),
        )
        for ((status, body, expected) in cases) {
            var requests = 0
            val client = ApiHttpClient().create(MockEngine {
                requests++
                respond(
                    body,
                    HttpStatusCode.fromValue(status),
                    headersOf(HttpHeaders.ContentType, "application/problem+json"),
                )
            })
            try {
                val failure = assertFailsWith<RegistrationException> {
                    KtorRegistrationApi(client, configuration).register(account)
                }
                assertEquals(expected, failure.error)
                assertFalse(failure.toString().contains("private"))
                assertEquals(1, requests)
                if (expected == RegistrationError.INVALID_INPUT && body == validation) {
                    assertEquals(setOf("login", "email"), failure.fieldErrors)
                }
            } finally {
                client.close()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun requestTimeoutIsUncertainAndSentOnce() = runTest {
        var requests = 0
        val engine = MockEngine.create {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                requests++
                delay(20_000)
                respond("", HttpStatusCode.Created)
            }
        }
        val client = ApiHttpClient().create(engine)
        try {
            val failure = assertFailsWith<RegistrationException> {
                KtorRegistrationApi(client, configuration).register(account)
            }
            assertEquals(RegistrationError.UNCERTAIN, failure.error)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun accountRepresentationDoesNotExposeThePassword() {
        assertFalse(account.toString().contains("secret"))
        assertFalse(account.toString().contains("🔐"))
    }
}
