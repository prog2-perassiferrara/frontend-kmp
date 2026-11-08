package com.prog2.frontend.network

import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import com.prog2.frontend.session.data.KtorAuthApi
import com.prog2.frontend.session.testToken
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
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class KtorAuthApiTest {
    private val configuration = ApiConfiguration("https://turnos.test", "https://catalog.test/")

    @Test
    fun postsExactCredentialsWithoutBearerAndAcceptsCompatibleFields() = runTest {
        val token = testToken()
        var requests = 0
        val client = ApiHttpClient().create(MockEngine { request ->
            requests++
            assertEquals("/api/authenticate", request.url.encodedPath)
            assertEquals("turnos.test", request.url.host)
            assertNull(request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals(" USER@EXAMPLE.TEST ", body["username"]!!.jsonPrimitive.content)
            assertEquals("  🔐secret  ", body["password"]!!.jsonPrimitive.content)
            assertEquals(true, body["rememberMe"]!!.jsonPrimitive.boolean)
            respond("""{"id_token":"$token","extra":"compatible"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            assertEquals(token, KtorAuthApi(client, configuration).authenticate(" USER@EXAMPLE.TEST ", "  🔐secret  ", true))
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test
    fun mapsStatusAndCodeWithoutShowingServerDetailsOrRetrying() = runTest {
        val cases = listOf(
            Triple(400, """{"code":"VALIDATION_ERROR","detail":"private"}""", SessionError.INVALID_INPUT),
            Triple(401, """{"code":"INVALID_CREDENTIALS"}""", SessionError.INVALID_CREDENTIALS),
            Triple(401, "no-json", SessionError.INVALID_CREDENTIALS),
            Triple(401, """{"code":"UNEXPECTED"}""", SessionError.UNAVAILABLE),
            Triple(503, """{"code":"AUTHENTICATION_UNAVAILABLE"}""", SessionError.UNAVAILABLE),
            Triple(500, "", SessionError.UNAVAILABLE),
            Triple(200, "{}", SessionError.INVALID_RESPONSE),
            Triple(200, """{"id_token":""}""", SessionError.INVALID_RESPONSE),
        )
        for ((status, body, expected) in cases) {
            var requests = 0
            val client = ApiHttpClient().create(MockEngine {
                requests++
                respond(body, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, "application/problem+json"))
            })
            try {
                val failure = assertFailsWith<SessionException> {
                    KtorAuthApi(client, configuration).authenticate("user", "secret", false)
                }
                assertEquals(expected, failure.error)
                assertFalse(failure.toString().contains("private"))
                assertEquals(1, requests)
            } finally { client.close() }
        }
    }

    @Test
    fun aRedirectDoesNotForwardCredentialsToAnotherHost() = runTest {
        var requests = 0
        val client = ApiHttpClient().create(MockEngine {
            requests++
            respond("", HttpStatusCode.TemporaryRedirect, headersOf(HttpHeaders.Location, "https://other.test/login"))
        })
        try {
            assertEquals(SessionError.UNAVAILABLE, assertFailsWith<SessionException> {
                KtorAuthApi(client, configuration).authenticate("user", "secret", false)
            }.error)
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun requestTimeoutFailsOnceWithoutGrantingAccess() = runTest {
        var requests = 0
        val engine = MockEngine.create {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { requests++; delay(20_000); respond("{}") }
        }
        val client = ApiHttpClient().create(engine)
        try {
            assertEquals(SessionError.UNAVAILABLE, assertFailsWith<SessionException> {
                KtorAuthApi(client, configuration).authenticate("user", "secret", false)
            }.error)
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test
    fun rejectsUnapprovedOrigins() {
        for (url in listOf(
            "http://remote.test", "https://user:password@turnos.test", "https://turnos.test/path",
            "https://turnos.test/?query=1", "https://turnos.test/#fragment",
        )) {
            assertFailsWith<IllegalArgumentException> { ApiConfiguration(url, "https://catalog.test") }
        }
        ApiConfiguration("http://10.0.2.2:8081", "http://10.0.2.2:8080", allowLocalHttp = true)
    }
}
