package com.prog2.frontend.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    private fun manager(
        authApi: TestAuthApi = TestAuthApi(),
        store: MemorySessionStore = MemorySessionStore(),
        clock: TestClock = TestClock(),
    ) = SessionManager(authApi, store, JwtExpiry(), clock)

    @Test
    fun restoresOnlyOnceWithoutCallingAuthentication() = runTest {
        val authApi = TestAuthApi()
        val store = MemorySessionStore(testToken())
        val manager = manager(authApi, store)
        manager.restore()
        manager.restore()
        assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
        assertEquals(1, store.reads)
        assertEquals(0, authApi.requests)
    }

    @Test
    fun restoresAnEmptyStoreAsSignedOut() = runTest {
        val manager = manager()
        manager.restore()
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
    }

    @Test
    fun readFailureDoesNotGrantAccessAndCleansTheUnusableStore() = runTest {
        val authApi = TestAuthApi()
        val store = MemorySessionStore(testToken()).apply { failRead = true }
        val manager = manager(authApi, store)
        manager.restore()
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        assertEquals(SessionError.STORAGE_UNAVAILABLE, manager.state.value.error)
        assertNull(store.saved)
        assertEquals(0, authApi.requests)
    }

    @Test
    fun discardsExpiredAndMalformedStoredTokens() = runTest {
        for (saved in listOf(testToken(expiresAt = 1_000), "invalid")) {
            val store = MemorySessionStore(saved)
            val manager = manager(store = store)
            manager.restore()
            assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
            assertNull(store.saved)
        }
    }

    @Test
    fun persistsTheReceivedExpiryAndRejectsItAfterExpiration() = runTest {
        for (duration in listOf(86_400L, 2_592_000L)) {
            val received = testToken(expiresAt = 1_000 + duration)
            val store = MemorySessionStore()
            val authApi = TestAuthApi { _, _, _ -> received }
            val clock = TestClock()
            val manager = manager(authApi, store, clock)
            manager.signIn("user", "secret", duration > 86_400)
            assertEquals(received, store.saved)
            val reopened = manager(authApi, store, clock)
            reopened.restore()
            assertEquals(SessionStatus.SIGNED_IN, reopened.state.value.status)
            clock.seconds = 1_000 + duration
            reopened.checkValidity()
            assertEquals(SessionStatus.SIGNED_OUT, reopened.state.value.status)
            assertNull(store.saved)
            assertEquals(1, authApi.requests)
        }
    }

    @Test
    fun doesNotPersistRejectedOrUnusableAuthentication() = runTest {
        for (failure in listOf(SessionError.INVALID_CREDENTIALS, SessionError.UNAVAILABLE)) {
            val store = MemorySessionStore()
            val manager = manager(TestAuthApi { _, _, _ -> throw SessionException(failure) }, store)
            assertEquals(failure, assertFailsWith<SessionException> { manager.signIn("user", "secret", false) }.error)
            assertNull(store.saved)
            assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        }
        val store = MemorySessionStore()
        val manager = manager(TestAuthApi { _, _, _ -> testToken(expiresAt = 999) }, store)
        assertEquals(
            SessionError.INVALID_RESPONSE,
            assertFailsWith<SessionException> { manager.signIn("user", "secret", false) }.error,
        )
        assertNull(store.saved)
    }

    @Test
    fun failedPersistenceDoesNotPublishASession() = runTest {
        val store = MemorySessionStore().apply { failSave = true }
        val manager = manager(store = store)
        assertEquals(
            SessionError.STORAGE_UNAVAILABLE,
            assertFailsWith<SessionException> { manager.signIn("user", "secret", false) }.error,
        )
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        assertNull(store.saved)
    }

    @Test
    fun expiryIsCheckedAtControlPointsWithoutAnIdleTimer() = runTest {
        val clock = TestClock()
        val store = MemorySessionStore()
        val manager = manager(store = store, clock = clock)
        manager.signIn("user", "secret", false)
        clock.seconds = 2_000
        runCurrent()
        assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
        manager.checkValidity()
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        assertNull(store.saved)
    }

    @Test
    fun logoutClearsLocallyWithoutAuthenticatingAgain() = runTest {
        val authApi = TestAuthApi()
        val store = MemorySessionStore()
        val manager = manager(authApi, store)
        manager.signIn("user", "secret", false)
        manager.signOut()
        assertNull(store.saved)
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        assertEquals(1, authApi.requests)
    }

    @Test
    fun cleanupFailureWithdrawsAccessAndBlocksLoginUntilAStoreClearSucceeds() = runTest {
        val authApi = TestAuthApi()
        val store = MemorySessionStore()
        val manager = manager(authApi, store)
        manager.signIn("user", "secret", false)
        store.failClear = true
        manager.signOut()
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        assertEquals(SessionError.STORAGE_UNAVAILABLE, manager.state.value.error)
        assertEquals("", store.saved)
        assertEquals(
            SessionError.STORAGE_UNAVAILABLE,
            assertFailsWith<SessionException> { manager.signIn("other", "secret", false) }.error,
        )
        assertEquals(1, authApi.requests)
        store.failClear = false
        manager.signIn("other", "secret", false)
        assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
        assertEquals(testToken(), store.saved)
        assertEquals(2, authApi.requests)
    }

    @Test
    fun failedCleanupCannotRestoreTheClosedSessionInANewProcess() = runTest {
        val store = MemorySessionStore()
        val first = manager(store = store)
        first.signIn("user", "secret", false)
        store.failClear = true
        first.signOut()
        assertEquals(SessionStatus.SIGNED_OUT, first.state.value.status)

        store.failClear = false
        val reopened = manager(store = store)
        reopened.restore()
        assertEquals(SessionStatus.SIGNED_OUT, reopened.state.value.status)
        assertNull(store.saved)
    }

    @Test
    fun lateLoginCannotReplaceANewSession() = runTest {
        val pending = CompletableDeferred<Unit>()
        val authApi = TestAuthApi { username, _, _ ->
            if (username == "old") {
                pending.await()
                testToken(expiresAt = 2_000)
            } else {
                testToken(expiresAt = 3_000)
            }
        }
        val store = MemorySessionStore()
        val manager = manager(authApi, store)
        val old = async {
            assertFailsWith<SessionException> { manager.signIn("old", "secret", false) }.error
        }
        runCurrent()
        manager.signOut()
        manager.signIn("new", "secret", false)
        pending.complete(Unit)
        assertEquals(SessionError.SESSION_CHANGED, old.await())
        assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
        assertEquals(testToken(expiresAt = 3_000), store.saved)
    }

    @Test
    fun logoutWaitsForAnAcceptedWriteAndThenRemovesIt() = runTest {
        val gate = CompletableDeferred<Unit>()
        val store = MemorySessionStore().apply { saveGate = gate }
        val manager = manager(store = store)
        val login = async { manager.signIn("user", "secret", false) }
        runCurrent()
        val logout = async { manager.signOut() }
        runCurrent()
        assertFalse(logout.isCompleted)
        gate.complete(Unit)
        login.await()
        logout.await()
        assertNull(store.saved)
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
    }
}
