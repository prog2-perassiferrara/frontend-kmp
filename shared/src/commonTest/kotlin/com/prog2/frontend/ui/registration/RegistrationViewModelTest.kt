package com.prog2.frontend.ui.registration

import androidx.lifecycle.ViewModelStore
import com.prog2.frontend.registration.RegistrationError
import com.prog2.frontend.registration.RegistrationException
import com.prog2.frontend.registration.data.NewAccount
import com.prog2.frontend.registration.data.RegistrationApi
import com.prog2.frontend.session.JwtExpiry
import com.prog2.frontend.session.MemorySessionStore
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.session.TestAuthApi
import com.prog2.frontend.session.TestClock
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        owners.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun manager(authApi: TestAuthApi = TestAuthApi()) =
        SessionManager(authApi, MemorySessionStore(), JwtExpiry(), TestClock())

    private fun viewModel(api: RegistrationApi, manager: SessionManager): RegistrationViewModel =
        RegistrationViewModel(api, manager).also {
            owners += ViewModelStore().apply { put("registration", it) }
        }

    @Test
    fun validDataRegistersNormalizedAccountAndSignsInAutomatically() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi()
        val authApi = TestAuthApi()
        val manager = manager(authApi)
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        vm.changeLogin("PATIENT")
        vm.changeEmail("PATIENT@EXAMPLE.TEST")
        vm.changeFirstName(" Ana ")
        vm.changeLastName("Pérez")
        vm.changePassword(" secret ")
        vm.submit()
        runCurrent()

        val account = registrationApi.requests.single()
        assertEquals("patient", account.login)
        assertEquals("patient@example.test", account.email)
        assertEquals(" Ana ", account.firstName)
        assertEquals(" secret ", account.password)
        assertEquals("es", account.langKey)
        assertEquals(1, authApi.requests)
        assertEquals("patient", authApi.username)
        assertEquals(" secret ", authApi.password)
        assertEquals(false, authApi.rememberMe)
        assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
        assertEquals("", vm.state.value.password)
        assertFalse(vm.state.value.accountCreated)
    }

    @Test
    fun invalidFieldsBlockTheRequestAndShowFieldErrors() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi()
        val manager = manager()
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        vm.changeLogin("bad login")
        vm.changePassword("short")
        vm.changeFirstName("A")
        vm.changeLastName("   ")
        vm.changeEmail("missing-at")
        vm.submit()
        runCurrent()

        assertEquals(0, registrationApi.requests.size)
        val state = vm.state.value
        assertNotNull(state.loginError)
        assertNotNull(state.passwordError)
        assertNotNull(state.firstNameError)
        assertNotNull(state.lastNameError)
        assertNotNull(state.emailError)
        assertEquals("bad login", state.login)
        assertEquals("short", state.password)
    }

    @Test
    fun presentsBackendValidationErrorsByFieldWithoutSession() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi {
            throw RegistrationException(
                RegistrationError.INVALID_INPUT,
                mapOf("login" to "Pattern", "email" to "Email"),
            )
        }
        val authApi = TestAuthApi()
        val manager = manager(authApi)
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        fillValid(vm)
        vm.submit()
        runCurrent()

        assertNotNull(vm.state.value.loginError)
        assertNotNull(vm.state.value.emailError)
        assertNull(vm.state.value.formError)
        assertEquals(0, authApi.requests)
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
    }

    @Test
    fun backendValidationWithoutFieldsShowsAGeneralMessage() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi {
            throw RegistrationException(RegistrationError.INVALID_INPUT)
        }
        val manager = manager()
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        fillValid(vm)
        vm.submit()
        runCurrent()

        assertEquals(RegistrationError.INVALID_INPUT.message, vm.state.value.formError)
        assertNull(vm.state.value.loginError)
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
    }

    @Test
    fun duplicateIdentifierKeepsTheFormAndDoesNotAuthenticate() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi {
            throw RegistrationException(RegistrationError.IDENTIFIER_ALREADY_USED)
        }
        val authApi = TestAuthApi()
        val manager = manager(authApi)
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        fillValid(vm)
        vm.submit()
        runCurrent()

        assertEquals(RegistrationError.IDENTIFIER_ALREADY_USED.message, vm.state.value.formError)
        assertEquals("patient", vm.state.value.login)
        assertEquals("secret", vm.state.value.password)
        assertFalse(vm.state.value.accountCreated)
        assertEquals(0, authApi.requests)
    }

    @Test
    fun repeatedSubmitSendsOneRegistration() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val registrationApi = TestRegistrationApi().apply { this.gate = gate }
        val manager = manager()
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        fillValid(vm)
        repeat(3) { vm.submit() }
        runCurrent()

        assertEquals(1, registrationApi.requests.size)
        assertTrue(vm.state.value.isSubmitting)

        gate.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.isSubmitting)
        assertEquals(SessionStatus.SIGNED_IN, manager.state.value.status)
    }

    @Test
    fun failedAutomaticLoginReportsCreatedAccountAndNeverRepeatsRegistration() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi()
        val authApi = TestAuthApi { _, _, _ -> throw SessionException(SessionError.UNAVAILABLE) }
        val manager = manager(authApi)
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        fillValid(vm)
        vm.submit()
        runCurrent()

        assertTrue(vm.state.value.accountCreated)
        assertEquals("", vm.state.value.password)
        assertEquals(1, registrationApi.requests.size)
        assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)

        vm.submit()
        runCurrent()
        assertEquals(1, registrationApi.requests.size)
    }

    @Test
    fun uncertainResultClearsThePasswordWithoutAuthenticatingOrRetrying() = runTest(dispatcher) {
        val registrationApi = TestRegistrationApi {
            throw RegistrationException(RegistrationError.UNCERTAIN)
        }
        val authApi = TestAuthApi()
        val manager = manager(authApi)
        manager.restore()
        val vm = viewModel(registrationApi, manager)
        fillValid(vm)
        vm.submit()
        runCurrent()

        assertEquals(RegistrationError.UNCERTAIN.message, vm.state.value.formError)
        assertEquals("", vm.state.value.password)
        assertEquals("patient", vm.state.value.login)
        assertFalse(vm.state.value.accountCreated)
        assertEquals(1, registrationApi.requests.size)
        assertEquals(0, authApi.requests)
    }

    @Test
    fun leavingTheFormDiscardsThePassword() {
        val vm = viewModel(TestRegistrationApi(), manager())
        vm.changePassword("secret")
        assertFalse(vm.state.value.toString().contains("secret"))
        vm.reset()
        assertEquals("", vm.state.value.password)
    }

    private fun fillValid(vm: RegistrationViewModel) {
        vm.changeLogin("patient")
        vm.changePassword("secret")
        vm.changeFirstName("Ana")
        vm.changeLastName("Pérez")
        vm.changeEmail("patient@example.test")
    }

    private class TestRegistrationApi(
        var answer: suspend (NewAccount) -> Unit = {},
    ) : RegistrationApi {
        val requests = mutableListOf<NewAccount>()
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun register(account: NewAccount) {
            requests += account
            gate?.await()
            answer(account)
        }
    }
}
