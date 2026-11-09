package com.prog2.frontend.ui.session

import androidx.lifecycle.ViewModelStore
import com.prog2.frontend.session.JwtExpiry
import com.prog2.frontend.session.MemorySessionStore
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.session.TestAuthApi
import com.prog2.frontend.session.TestClock
import com.prog2.frontend.session.testToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<ViewModelStore>()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() {
        owners.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun manager(authApi: TestAuthApi = TestAuthApi()) =
        SessionManager(authApi, MemorySessionStore(), JwtExpiry(), TestClock())

    private fun viewModel(manager: SessionManager): SessionViewModel = SessionViewModel(manager).also {
        owners += ViewModelStore().apply { put("session", it) }
    }

    @Test
    fun validatesPasswordLengthWithoutChangingTheValue() = runTest(dispatcher) {
        val cases = listOf(
            "a".repeat(5) to false, "a".repeat(6) to true,
            "a".repeat(100) to true, "a".repeat(101) to false,
            "😀".repeat(2) to false, "😀".repeat(5) to false,
            "😀".repeat(6) to true, "😀".repeat(51) to true,
            "😀".repeat(100) to true, "😀".repeat(101) to false,
            "      " to true, "  🔐abc  " to true,
        )
        for ((password, valid) in cases) {
            val authApi = TestAuthApi()
            val manager = manager(authApi)
            manager.restore()
            val vm = viewModel(manager)
            runCurrent()
            vm.changeUsername("USER@EXAMPLE.TEST")
            vm.changePassword(password)
            vm.signIn()
            runCurrent()
            assertEquals(if (valid) 1 else 0, authApi.requests)
            if (valid) {
                assertEquals(password, authApi.password)
                assertEquals("user@example.test", authApi.username)
                assertEquals(false, authApi.rememberMe)
            } else assertNotNull(vm.state.value.passwordError)
        }
    }

    @Test
    fun validatesIdentifierLengthAfterNormalizationWithoutTrimming() = runTest(dispatcher) {
        for ((username, valid) in listOf(
            "" to false, "   " to false, "a".repeat(254) to true,
            "a".repeat(255) to false, "İ".repeat(128) to false, " user " to true,
        )) {
            val authApi = TestAuthApi()
            val manager = manager(authApi)
            manager.restore()
            val vm = viewModel(manager)
            runCurrent()
            vm.changeUsername(username)
            vm.changePassword("secret")
            vm.signIn()
            runCurrent()
            assertEquals(if (valid) 1 else 0, authApi.requests)
            if (valid) assertEquals(username.lowercase(), authApi.username)
            else assertNotNull(vm.state.value.usernameError)
        }
    }

    @Test
    fun repeatedSubmitKeepsOneAttemptAndSuccessfulLoginDiscardsTheForm() = runTest(dispatcher) {
        val answer = CompletableDeferred<String>()
        val authApi = TestAuthApi { _, _, _ -> answer.await() }
        val manager = manager(authApi)
        manager.restore()
        val vm = viewModel(manager)
        runCurrent()
        vm.changeUsername("user")
        vm.changePassword("secret")
        vm.changeRememberMe(true)
        repeat(3) { vm.signIn() }
        runCurrent()
        assertEquals(1, authApi.requests)
        assertTrue(vm.state.value.isSubmitting)
        assertEquals("secret", vm.state.value.password)
        assertEquals(true, authApi.rememberMe)
        answer.complete(testToken())
        runCurrent()
        assertEquals(SessionStatus.SIGNED_IN, vm.state.value.session.status)
        assertEquals("", vm.state.value.password)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun logoutDiscardsTheTemporaryForm() = runTest(dispatcher) {
        val manager = manager()
        manager.restore()
        val vm = viewModel(manager)
        runCurrent()
        vm.changeUsername("user")
        vm.changePassword("secret")
        vm.changeRememberMe(true)
        vm.signOut()
        runCurrent()
        assertEquals(SessionStatus.SIGNED_OUT, vm.state.value.session.status)
        assertEquals("", vm.state.value.username)
        assertEquals("", vm.state.value.password)
        assertFalse(vm.state.value.rememberMe)
    }

    @Test
    fun aNewViewModelDoesNotRecoverAnUnsentPassword() = runTest(dispatcher) {
        val manager = manager()
        manager.restore()
        val old = viewModel(manager)
        runCurrent()
        old.changeUsername("user")
        old.changePassword("secret")
        val replacement = viewModel(manager)
        assertEquals("", replacement.state.value.username)
        assertEquals("", replacement.state.value.password)
        assertFalse(old.state.value.toString().contains("secret"))
    }
}
