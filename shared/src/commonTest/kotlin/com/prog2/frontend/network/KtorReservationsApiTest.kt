package com.prog2.frontend.network

import com.prog2.frontend.reservation.ReservationException
import com.prog2.frontend.reservation.ReservationFailure
import com.prog2.frontend.reservation.data.KtorReservationsApi
import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.reservation.data.StartReservation
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
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorReservationsApiTest {
    private val configuration = ApiConfiguration("https://turnos.test", "https://catalog.test")
    private val processId = "b7273764-9bc3-43fd-b42c-8a9ca65c5639"

    private fun manager() = SessionManager(TestAuthApi(), MemorySessionStore(), JwtExpiry(), TestClock())

    private val request = StartReservation(
        requestId = processId,
        professionalId = 201,
        date = "2026-11-09",
        startTime = "12:30",
    )

    private val processJson = """
        {"processId":"$processId","professionalId":201,"date":"2026-11-09",
         "startTime":"12:30:00","state":"HOLD_PENDING","expiresAt":null,
         "reasonCode":null,"requiredField":null,"canSubmitPhone":false,
         "professionalFirstName":"Ana","professionalLastName":"Torres"}
    """.trimIndent()

    @Test
    fun startSendsTheContractBodyAndBearer() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        var requests = 0
        val client = ApiHttpClient().create(MockEngine { http ->
            requests++
            assertEquals(HttpMethod.Post, http.method)
            assertEquals("turnos.test", http.url.host)
            assertEquals("/api/reservation-processes", http.url.encodedPath)
            assertEquals("Bearer ${testToken()}", http.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement(http.body.toByteArray().decodeToString()).jsonObject
            assertEquals(processId, body["requestId"]!!.jsonPrimitive.content)
            assertEquals(201, body["professionalId"]!!.jsonPrimitive.int)
            assertEquals("2026-11-09", body["date"]!!.jsonPrimitive.content)
            assertEquals("12:30", body["startTime"]!!.jsonPrimitive.content)
            respond(processJson, HttpStatusCode.Accepted, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val process = KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                .start(request)
            assertEquals(processId, process.id)
            assertEquals("HOLD_PENDING", process.state)
            assertEquals("Ana Torres", process.professionalName)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun aTwoHundredIsAcceptedAsTheSameProcess() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        var requests = 0
        val client = ApiHttpClient().create(MockEngine {
            requests++
            respond(processJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val process = KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                .start(request)
            assertEquals(processId, process.id)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun mapsKnownRejectionsAndKeepsTheProcessWithoutRetrying() = runTest {
        val cases = listOf(
            Case(400, """{"code":"INVALID_RESERVATION_SELECTION"}""", ReservationFailure.REJECTED, null),
            Case(400, """{"code":"PROFILE_NOT_RESERVABLE"}""", ReservationFailure.REJECTED, null),
            Case(409, """{"code":"ACTIVE_RESERVATION_PROCESS","processId":"$processId"}""",
                ReservationFailure.REJECTED, processId),
            Case(409, """{"code":"REQUEST_ID_CONFLICT"}""", ReservationFailure.REJECTED, null),
            Case(409, """{"code":"SLOT_NOT_AVAILABLE"}""", ReservationFailure.REJECTED, null),
            Case(409, """{"code":"HOLD_EXPIRED"}""", ReservationFailure.REJECTED, null),
            Case(409, """{"code":"CONFIRMATION_REJECTED"}""", ReservationFailure.REJECTED, null),
            Case(403, """{"code":"ACCESS_DENIED"}""", ReservationFailure.REJECTED, null),
            Case(503, """{"code":"CATALOG_UNAVAILABLE"}""", ReservationFailure.UNCERTAIN, null),
            Case(503, """{"code":"HOLD_CREATION_UNCERTAIN","processId":"$processId"}""",
                ReservationFailure.UNCERTAIN, processId),
            Case(500, """{"code":"INTERNAL_ERROR"}""", ReservationFailure.UNCERTAIN, null),
            Case(502, "", ReservationFailure.UNCERTAIN, null),
        )
        for (case in cases) {
            val manager = manager()
            manager.signIn("patient", "secret", false)
            var requests = 0
            val client = ApiHttpClient().create(MockEngine {
                requests++
                respond(
                    case.body,
                    HttpStatusCode.fromValue(case.status),
                    headersOf(HttpHeaders.ContentType, "application/problem+json"),
                )
            })
            try {
                val failure = assertFailsWith<ReservationException> {
                    KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                        .start(request)
                }
                assertEquals(case.failure, failure.failure, "status=${case.status} body=${case.body}")
                assertEquals(case.processId, failure.processId)
                assertTrue(failure.message?.isNotBlank() == true)
                assertEquals(1, requests)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun aTransportFailureOnStartIsUncertain() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { throw RuntimeException("network") })
        try {
            val failure = assertFailsWith<ReservationException> {
                KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager)).start(request)
            }
            assertEquals(ReservationFailure.UNCERTAIN, failure.failure)
            assertNull(failure.processId)
        } finally {
            client.close()
        }
    }

    @Test
    fun processReadsTheDurableProgressWithTheUserBearer() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        var requests = 0
        val client = ApiHttpClient().create(MockEngine { http ->
            requests++
            assertEquals(HttpMethod.Get, http.method)
            assertEquals("/api/reservation-processes/$processId", http.url.encodedPath)
            assertEquals("Bearer ${testToken()}", http.headers[HttpHeaders.Authorization])
            respond(
                """{"processId":"$processId","professionalId":201,"date":"2026-11-09",
                    "startTime":"12:30:00","state":"WAITING_FOR_PHONE",
                    "expiresAt":"2026-11-09T15:30:00Z","reasonCode":null,
                    "requiredField":"PHONE_NUMBER","canSubmitPhone":true,
                    "professionalFirstName":null,"professionalLastName":null,
                    "phoneRequestId":"ignored","warningCode":"ignored"}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        try {
            val process = KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                .process(processId)
            assertEquals(
                ReservationProcess(
                    id = processId,
                    professionalId = 201,
                    date = "2026-11-09",
                    startTime = "12:30:00",
                    state = "WAITING_FOR_PHONE",
                    expiresAt = "2026-11-09T15:30:00Z",
                    reasonCode = null,
                    requiredField = "PHONE_NUMBER",
                    canSubmitPhone = true,
                    professionalFirstName = null,
                    professionalLastName = null,
                ),
                process,
            )
            assertTrue(process.phoneRequired)
            assertFalse(process.isFinal)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun onlyAnAuthenticNotFoundEnablesRetryingTheStart() = runTest {
        val cases = listOf(
            Triple(404, "", ReservationFailure.NOT_FOUND),
            Triple(400, """{"code":"RESERVATION_PROCESS_NOT_FOUND"}""", ReservationFailure.NOT_FOUND),
            Triple(403, "", ReservationFailure.UNCERTAIN),
            Triple(400, "", ReservationFailure.UNCERTAIN),
            Triple(503, "", ReservationFailure.UNCERTAIN),
            Triple(500, "", ReservationFailure.UNCERTAIN),
        )
        for ((status, body, expected) in cases) {
            val manager = manager()
            manager.signIn("patient", "secret", false)
            val client = ApiHttpClient().create(MockEngine {
                respond(body, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, "application/problem+json"))
            })
            try {
                val failure = assertFailsWith<ReservationException> {
                    KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                        .process(processId)
                }
                assertEquals(expected, failure.failure, "status=$status body=$body")
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun anUnexpectedBodyOrTransportOnProcessIsUncertain() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { respond("no-json", HttpStatusCode.OK) })
        try {
            assertEquals(
                ReservationFailure.UNCERTAIN,
                assertFailsWith<ReservationException> {
                    KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                        .process(processId)
                }.failure,
            )
        } finally {
            client.close()
        }

        val other = manager()
        other.signIn("patient", "secret", false)
        val offline = ApiHttpClient().create(MockEngine { throw RuntimeException("network") })
        try {
            assertEquals(
                ReservationFailure.UNCERTAIN,
                assertFailsWith<ReservationException> {
                    KtorReservationsApi(AuthenticatedApiClient(offline, configuration, other))
                        .process(processId)
                }.failure,
            )
        } finally {
            offline.close()
        }
    }

    @Test
    fun processRejectsANonUuidIdentifierWithoutCallingTurnos() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        var requests = 0
        val client = ApiHttpClient().create(MockEngine { requests++; respond(processJson) })
        try {
            val failure = assertFailsWith<ReservationException> {
                KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
                    .process("../otro")
            }
            assertEquals(ReservationFailure.NOT_FOUND, failure.failure)
            assertEquals(0, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun pendingReadsTheOwnProcessAndAnAbsentPendingIsNull() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        var requests = 0
        var body = """{"pending":$processJson,"history":{"content":[],"page":0,"size":1,"totalElements":0,"totalPages":0}}"""
        val client = ApiHttpClient().create(MockEngine { http ->
            requests++
            assertEquals(HttpMethod.Get, http.method)
            assertEquals("/api/reservations", http.url.encodedPath)
            assertEquals("0", http.url.parameters["page"])
            assertEquals("1", http.url.parameters["size"])
            assertEquals("Bearer ${testToken()}", http.headers[HttpHeaders.Authorization])
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val api = KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager))
            assertEquals(processId, api.pending()?.id)
            body = """{"pending":null,"history":{"content":[],"page":0,"size":1,"totalElements":0,"totalPages":0}}"""
            assertNull(api.pending())
            assertEquals(2, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun pendingFailureAndTransportAreUncertainReads() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine {
            respond("", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/problem+json"))
        })
        try {
            assertEquals(
                ReservationFailure.UNCERTAIN,
                assertFailsWith<ReservationException> {
                    KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager)).pending()
                }.failure,
            )
        } finally {
            client.close()
        }

        val other = manager()
        other.signIn("patient", "secret", false)
        val offline = ApiHttpClient().create(MockEngine { throw RuntimeException("network") })
        try {
            assertEquals(
                ReservationFailure.UNCERTAIN,
                assertFailsWith<ReservationException> {
                    KtorReservationsApi(AuthenticatedApiClient(offline, configuration, other)).pending()
                }.failure,
            )
        } finally {
            offline.close()
        }
    }

    @Test
    fun a401ClosesTheSessionThroughTheExistingClient() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        try {
            val failure = assertFailsWith<SessionException> {
                KtorReservationsApi(AuthenticatedApiClient(client, configuration, manager)).start(request)
            }
            assertEquals(SessionError.SESSION_REQUIRED, failure.error)
            assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        } finally {
            client.close()
        }
    }

    private data class Case(
        val status: Int,
        val body: String,
        val failure: ReservationFailure,
        val processId: String?,
    )
}
