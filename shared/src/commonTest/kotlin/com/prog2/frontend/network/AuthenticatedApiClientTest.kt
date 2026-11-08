package com.prog2.frontend.network

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthenticatedApiClientTest {
    private val configuration = ApiConfiguration("https://turnos.test", "https://catalog.test")

    private fun manager(
        authApi: TestAuthApi = TestAuthApi(),
        store: MemorySessionStore = MemorySessionStore(),
        clock: TestClock = TestClock(),
    ) = SessionManager(authApi, store, JwtExpiry(), clock)

    @Test
    fun sendsTheSameUserTokenToBothOwnBackendsAndPreservesQuery() = runTest {
        val manager = manager()
        manager.signIn("user", "secret", false)
        val hosts = mutableListOf<String>()
        val client = ApiHttpClient().create(MockEngine { request ->
            hosts += request.url.host
            assertEquals("Bearer ${testToken()}", request.headers[HttpHeaders.Authorization])
            assertEquals("with space", request.url.parameters["value"])
            respond("[]")
        })
        try {
            val protectedApi = AuthenticatedApiClient(client, configuration, manager)
            protectedApi.get(Backend.TURNOS, "/api/protected", mapOf("value" to "with space"))
            protectedApi.get(Backend.CATALOG, "/api/protected", mapOf("value" to "with space"))
            assertEquals(listOf("turnos.test", "catalog.test"), hosts)
        } finally { client.close() }
    }

    @Test
    fun knownExpiryPreventsAnyHttpRequest() = runTest {
        val clock = TestClock()
        val store = MemorySessionStore()
        val manager = manager(store = store, clock = clock)
        manager.signIn("user", "secret", false)
        clock.seconds = 2_000
        var requests = 0
        val client = ApiHttpClient().create(MockEngine { requests++; respond("unexpected") })
        try {
            assertEquals(SessionError.SESSION_REQUIRED, assertFailsWith<SessionException> {
                AuthenticatedApiClient(client, configuration, manager).get(Backend.TURNOS, "/api/protected")
            }.error)
            assertEquals(0, requests)
            assertNull(store.saved)
        } finally { client.close() }
    }

    @Test
    fun onlyUserAuthenticationRejectionClearsTheSession() = runTest {
        for (status in listOf(401, 403, 503)) {
            val store = MemorySessionStore()
            val manager = manager(store = store)
            manager.signIn("user", "secret", false)
            val client = ApiHttpClient().create(MockEngine { respond("", HttpStatusCode.fromValue(status)) })
            try {
                val protectedApi = AuthenticatedApiClient(client, configuration, manager)
                if (status == 401) {
                    assertFailsWith<SessionException> { protectedApi.get(Backend.TURNOS, "/api/protected") }
                    assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
                    assertNull(store.saved)
                } else {
                    assertEquals(status, protectedApi.get(Backend.TURNOS, "/api/protected").status)
                    assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
                    assertEquals(testToken(), store.saved)
                }
            } finally { client.close() }
        }
    }

    @Test
    fun oldResponsesCannotPublishDataOrCloseANewSession() = runTest {
        for (status in listOf(200, 401)) {
            val started = CompletableDeferred<Unit>()
            val answer = CompletableDeferred<Unit>()
            val authApi = TestAuthApi { username, _, _ ->
                testToken(if (username == "a") 2_000 else 3_000)
            }
            val store = MemorySessionStore()
            val manager = manager(authApi, store)
            manager.signIn("a", "secret", false)
            val client = ApiHttpClient().create(MockEngine {
                started.complete(Unit)
                answer.await()
                respond("old private data", HttpStatusCode.fromValue(status))
            })
            try {
                val result = async {
                    assertFailsWith<SessionException> {
                        AuthenticatedApiClient(client, configuration, manager).get(Backend.TURNOS, "/api/protected")
                    }.error
                }
                started.await()
                manager.signOut()
                manager.signIn("b", "secret", false)
                answer.complete(Unit)
                assertEquals(SessionError.SESSION_CHANGED, result.await())
                assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
                assertEquals(testToken(3_000), store.saved)
            } finally { client.close() }
        }
    }

    @Test
    fun successfulInFlightResponseIsNotRejectedOnlyBecauseTimePassed() = runTest {
        val clock = TestClock()
        val manager = manager(clock = clock)
        manager.signIn("user", "secret", false)
        val client = ApiHttpClient().create(MockEngine { clock.seconds = 2_001; respond("accepted") })
        try {
            assertEquals("accepted", AuthenticatedApiClient(client, configuration, manager).get(Backend.TURNOS, "/api/protected").body)
            assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
        } finally { client.close() }
    }

    @Test
    fun transportFailurePreservesTheCurrentSession() = runTest {
        val store = MemorySessionStore()
        val manager = manager(store = store)
        manager.signIn("user", "secret", false)
        val client = ApiHttpClient().create(MockEngine { throw IOException("controlled transport failure") })
        try {
            assertEquals(SessionError.UNAVAILABLE, assertFailsWith<SessionException> {
                AuthenticatedApiClient(client, configuration, manager).get(Backend.TURNOS, "/api/protected")
            }.error)
            assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
            assertEquals(testToken(), store.saved)
        } finally { client.close() }
    }

    @Test
    fun rejectsPathsOutsideTheApiBeforeSending() = runTest {
        var requests = 0
        val manager = manager()
        manager.signIn("user", "secret", false)
        val client = ApiHttpClient().create(MockEngine { requests++; respond("") })
        try {
            for (path in listOf("https://other.test/api/data", "api/protected", "/other")) {
                assertFailsWith<IllegalArgumentException> {
                    AuthenticatedApiClient(client, configuration, manager).get(Backend.TURNOS, path)
                }
            }
            assertEquals(0, requests)
        } finally { client.close() }
    }
}
