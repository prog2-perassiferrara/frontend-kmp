package com.prog2.frontend.network

import com.prog2.frontend.availability.AvailabilityError
import com.prog2.frontend.availability.AvailabilityException
import com.prog2.frontend.availability.data.KtorAvailabilityApi
import com.prog2.frontend.availability.data.ProfessionalAvailability
import com.prog2.frontend.session.JwtExpiry
import com.prog2.frontend.session.MemorySessionStore
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.session.TestAuthApi
import com.prog2.frontend.session.TestClock
import com.prog2.frontend.session.testToken
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KtorAvailabilityApiTest {
    private val configuration = ApiConfiguration("https://turnos.test", "https://catalog.test")

    private fun manager() = SessionManager(TestAuthApi(), MemorySessionStore(), JwtExpiry(), TestClock())

    private val availabilityJson = """
        {"professionalId":201,"date":"2026-11-09",
         "timeZone":"America/Argentina/Buenos_Aires",
         "availableStartTimes":["12:00:00","12:30:00","13:30:00"]}
    """.trimIndent()

    @Test
    fun asksTurnosWithTheProfessionalAndDateUsingTheUserBearer() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { request ->
            assertEquals("turnos.test", request.url.host)
            assertEquals("/api/appointment-availability", request.url.encodedPath)
            assertEquals("Bearer ${testToken()}", request.headers[HttpHeaders.Authorization])
            assertEquals("201", request.url.parameters["professionalId"])
            assertEquals("2026-11-09", request.url.parameters["date"])
            respond(availabilityJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val availability = KtorAvailabilityApi(AuthenticatedApiClient(client, configuration, manager))
                .availability(201, "2026-11-09")
            assertEquals(
                ProfessionalAvailability(
                    professionalId = 201,
                    date = "2026-11-09",
                    timeZone = "America/Argentina/Buenos_Aires",
                    startTimes = listOf("12:00:00", "12:30:00", "13:30:00"),
                ),
                availability,
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun anEmptyListIsAValidAnswerWithoutError() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine {
            respond(
                """{"professionalId":201,"date":"2026-11-09","timeZone":"America/Argentina/Buenos_Aires","availableStartTimes":[]}""",
            )
        })
        try {
            val availability = KtorAvailabilityApi(AuthenticatedApiClient(client, configuration, manager))
                .availability(201, "2026-11-09")
            assertTrue(availability.startTimes.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun aTwoHundredWithoutAvailableStartTimesIsAnErrorNotAnEmptyList() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine {
            respond(
                """{"professionalId":201,"date":"2026-11-09","timeZone":"America/Argentina/Buenos_Aires"}""",
            )
        })
        try {
            val failure = assertFailsWith<AvailabilityException> {
                KtorAvailabilityApi(AuthenticatedApiClient(client, configuration, manager))
                    .availability(201, "2026-11-09")
            }
            assertEquals(AvailabilityError.UNAVAILABLE, failure.error)
        } finally {
            client.close()
        }
    }

    @Test
    fun mapsRejectionsWithoutRetryingTheHttpCall() = runTest {
        val cases = listOf(
            Triple(400, """{"code":"INVALID_AVAILABILITY_QUERY"}""", AvailabilityError.INVALID_QUERY),
            Triple(400, """{"code":"PROFESSIONAL_DISABLED"}""", AvailabilityError.PROFESSIONAL_UNAVAILABLE),
            Triple(404, """{"code":"PROFESSIONAL_NOT_FOUND"}""", AvailabilityError.NOT_FOUND),
            Triple(503, """{"code":"CATALOG_NOT_CURRENT"}""", AvailabilityError.UNAVAILABLE),
            Triple(503, """{"code":"OCCUPANCIES_UNAVAILABLE"}""", AvailabilityError.UNAVAILABLE),
            Triple(500, """{"code":"INTERNAL_ERROR"}""", AvailabilityError.UNAVAILABLE),
            Triple(200, "no-json", AvailabilityError.UNAVAILABLE),
        )
        for ((status, body, expected) in cases) {
            val manager = manager()
            manager.signIn("patient", "secret", false)
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
                val failure = assertFailsWith<AvailabilityException> {
                    KtorAvailabilityApi(AuthenticatedApiClient(client, configuration, manager))
                        .availability(201, "2026-11-09")
                }
                assertEquals(expected, failure.error, "status=$status body=$body")
                assertEquals(1, requests)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun a401ClosesTheSessionThroughTheExistingClient() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        try {
            val failure = assertFailsWith<SessionException> {
                KtorAvailabilityApi(AuthenticatedApiClient(client, configuration, manager))
                    .availability(201, "2026-11-09")
            }
            assertEquals(SessionError.SESSION_REQUIRED, failure.error)
            assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        } finally {
            client.close()
        }
    }
}
