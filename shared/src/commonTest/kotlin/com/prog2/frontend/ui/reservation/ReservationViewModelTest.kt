package com.prog2.frontend.ui.reservation

import androidx.lifecycle.ViewModelStore
import com.prog2.frontend.reservation.ReservationException
import com.prog2.frontend.reservation.ReservationFailure
import com.prog2.frontend.reservation.data.OwnReservationsPage
import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.reservation.data.ReservationSelection
import com.prog2.frontend.reservation.data.ReservationsApi
import com.prog2.frontend.reservation.data.StartReservation
import com.prog2.frontend.reservation.data.SubmitPhone
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
class ReservationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<ViewModelStore>()

    private val selection = ReservationSelection(
        professionalId = 201,
        date = "2026-11-09",
        startTime = "12:30:00",
        professionalName = "Ana Torres",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        owners.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun viewModel(api: ReservationsApi, vararg requestIds: String): ReservationViewModel {
        val ids = requestIds.ifEmpty { arrayOf(REQUEST_ID) }
        var index = 0
        return ReservationViewModel(api) { ids[minOf(index++, ids.lastIndex)] }.also {
            owners += ViewModelStore().apply { put("reservation", it) }
        }
    }

    private fun process(
        id: String = REQUEST_ID,
        state: String = "HOLD_PENDING",
        requiredField: String? = null,
        canSubmitPhone: Boolean = false,
    ) = ReservationProcess(
        id = id,
        professionalId = 201,
        date = "2026-11-09",
        startTime = "12:30:00",
        state = state,
        expiresAt = "2026-11-09T15:30:00Z",
        reasonCode = null,
        requiredField = requiredField,
        canSubmitPhone = canSubmitPhone,
        professionalFirstName = "Ana",
        professionalLastName = "Torres",
    )

    @Test
    fun startSendsTheSelectionOnceAndShowsTheReportedProcess() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = viewModel(api)

        vm.start(selection)
        assertEquals(StartStatus.SENDING, vm.state.value.start)
        assertNull(vm.state.value.process)
        // El identificador de la acción existe antes de la respuesta y no cambia.
        assertEquals(REQUEST_ID, vm.state.value.requestId)
        runCurrent()

        val started = vm.state.value
        assertEquals(process(), started.process)
        assertEquals(StartStatus.IDLE, started.start)
        assertEquals(REQUEST_ID, started.requestId)
        assertEquals("Ana Torres", started.professionalName)
        assertEquals(1, api.starts.size)
        assertEquals(REQUEST_ID, api.starts.single().requestId)
        assertEquals(201, api.starts.single().professionalId)
        assertEquals("2026-11-09", api.starts.single().date)
        assertEquals("12:30:00", api.starts.single().startTime)
    }

    @Test
    fun aSecondStartWhileTheRequestIsInFlightDoesNotSendAnotherOne() = runTest(dispatcher) {
        val api = FakeApi()
        val response = CompletableDeferred<ReservationProcess>()
        api.onStart = { response.await() }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()
        vm.start(selection)
        runCurrent()
        assertEquals(1, api.starts.size)
        assertEquals(StartStatus.SENDING, vm.state.value.start)

        response.complete(process())
        runCurrent()
        assertEquals(process(), vm.state.value.process)
        assertEquals(1, api.starts.size)
    }

    @Test
    fun aKnownRejectionIsPresentedWithoutRepeatingTheStart() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw rejected("El horario dejó de estar disponible.") }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()

        val state = vm.state.value
        assertNull(state.process)
        assertEquals(StartStatus.REJECTED, state.start)
        assertEquals("El horario dejó de estar disponible.", state.message)
        assertEquals(1, api.starts.size)
        assertEquals(0, api.processes.size)
    }

    @Test
    fun anErrorWithAProcessIdLoadsThatProcessInsteadOfStartingAnother() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw rejected(ACTIVE_MESSAGE, processId = REQUEST_ID) }
        api.onProcess = { process(state = "HELD") }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()

        val state = vm.state.value
        assertEquals(process(state = "HELD"), state.process)
        assertEquals(ACTIVE_MESSAGE, state.message)
        assertEquals(REQUEST_ID, state.requestId)
        assertEquals(1, api.starts.size)
        assertEquals(listOf(REQUEST_ID), api.processes)
    }

    @Test
    fun anActiveProcessWhoseReadFailsRemainsVerifiable() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw rejected(ACTIVE_MESSAGE, processId = REQUEST_ID) }
        api.onProcess = { throw uncertain(READ_MESSAGE) }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()

        // Sin el GET no se puede afirmar un rechazo definitivo ni repetir el POST.
        val state = vm.state.value
        assertNull(state.process)
        assertEquals(StartStatus.UNCERTAIN, state.start)
        assertEquals(REQUEST_ID, state.requestId)
        assertEquals(READ_MESSAGE, state.message)
        assertFalse(state.loading)

        vm.retryStart()
        runCurrent()
        assertEquals(1, api.starts.size)

        api.onProcess = { process(state = "HELD") }
        vm.verify()
        runCurrent()
        assertEquals(process(state = "HELD"), vm.state.value.process)
        assertEquals(2, api.processes.size)
    }

    @Test
    fun anUncertainStartIsNotPresentedAsSuccessNorAsFailure() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw uncertain(START_MESSAGE) }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()

        val state = vm.state.value
        assertNull(state.process)
        assertEquals(StartStatus.UNCERTAIN, state.start)
        assertEquals(START_MESSAGE, state.message)
        assertEquals(REQUEST_ID, state.requestId)
        assertEquals(BannerAction.SHOW_PROCESS, state.banner?.action)
        assertEquals(1, api.starts.size)
    }

    @Test
    fun verifyingAnUnregisteredStartAllowsRetryWithTheSameRequestId() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw uncertain(START_MESSAGE) }
        api.onProcess = { throw notFound() }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()
        vm.verify()
        runCurrent()
        assertEquals(StartStatus.NOT_REGISTERED, vm.state.value.start)
        assertEquals(listOf(REQUEST_ID), api.processes)

        // El reintento recién se habilita luego del 404 de verificación.
        api.onStart = { process() }
        vm.retryStart()
        runCurrent()

        assertEquals(process(), vm.state.value.process)
        assertEquals(2, api.starts.size)
        assertEquals(listOf(REQUEST_ID, REQUEST_ID), api.starts.map { it.requestId })
    }

    @Test
    fun verifyingAnExistingProcessShowsItWithoutInitiatingAgain() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw uncertain(START_MESSAGE) }
        api.onProcess = { process(state = "CONFIRMATION_PENDING") }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()
        vm.verify()
        runCurrent()

        assertEquals(process(state = "CONFIRMATION_PENDING"), vm.state.value.process)
        assertEquals(1, api.starts.size)
        assertEquals(listOf(REQUEST_ID), api.processes)
    }

    @Test
    fun aFailedVerificationKeepsTheUncertaintyAndDoesNotEnableAnotherStart() = runTest(dispatcher) {
        val api = FakeApi()
        api.onStart = { throw uncertain(START_MESSAGE) }
        api.onProcess = { throw uncertain(READ_MESSAGE) }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()
        vm.verify()
        runCurrent()

        val state = vm.state.value
        assertEquals(StartStatus.UNCERTAIN, state.start)
        assertEquals(READ_MESSAGE, state.message)
        assertFalse(state.loading)
        assertEquals(BannerAction.SHOW_PROCESS, state.banner?.action)

        // Un 5xx, un 403 o un 400 no acreditan inexistencia: no se repite el POST.
        vm.retryStart()
        runCurrent()
        assertEquals(1, api.starts.size)
        assertEquals(StartStatus.UNCERTAIN, vm.state.value.start)
    }

    @Test
    fun discoveryOpensTheOwnPendingProcessOnlyOnce() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { process() }
        val vm = viewModel(api)

        vm.discover()
        vm.discover()
        runCurrent()

        val state = vm.state.value
        assertEquals(process(), state.process)
        assertEquals(REQUEST_ID, state.requestId)
        assertEquals(1, api.pendings)
        assertEquals(0, api.starts.size)
    }

    @Test
    fun withoutAPendingProcessTheSearchRemainsAndNoBannerIsShown() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = viewModel(api)

        vm.discover()
        runCurrent()

        assertEquals(ReservationUiState(), vm.state.value)
        assertNull(vm.state.value.banner)
    }

    @Test
    fun aDiscoveryFailureDoesNotBlockTheSearchAndCanBeRetried() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { throw uncertain(READ_MESSAGE) }
        val vm = viewModel(api)

        vm.discover()
        runCurrent()

        // El fallo no se interpreta como ausencia de proceso.
        assertNull(vm.state.value.process)
        assertEquals(READ_MESSAGE, vm.state.value.message)
        val banner = assertNotNull(vm.state.value.banner)
        assertEquals(BannerAction.RETRY_DISCOVERY, banner.action)

        api.onPending = { process() }
        vm.retryDiscovery()
        runCurrent()

        assertEquals(process(), vm.state.value.process)
        assertEquals(2, api.pendings)
    }

    @Test
    fun aPendingProcessHiddenByTheUserRemainsAvailableThroughTheBanner() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { process() }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()

        vm.dismiss()

        assertEquals(process(), vm.state.value.process)
        assertEquals(BannerAction.SHOW_PROCESS, vm.state.value.banner?.action)
        assertEquals(0, api.processes.size)

        vm.refresh()
        runCurrent()
        assertEquals(listOf(REQUEST_ID), api.processes)
    }

    @Test
    fun dismissDiscardsFinalRejectedAndUnregisteredProcesses() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { process(state = "CONFIRMED") }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()
        vm.dismiss()
        assertEquals(ReservationUiState(), vm.state.value)

        val rejectedApi = FakeApi()
        rejectedApi.onStart = { throw rejected("No pudimos iniciar la reserva.") }
        val rejectedVm = viewModel(rejectedApi)
        rejectedVm.start(selection)
        runCurrent()
        rejectedVm.dismiss()
        assertEquals(ReservationUiState(), rejectedVm.state.value)

        val uncertainApi = FakeApi()
        uncertainApi.onStart = { throw uncertain(START_MESSAGE) }
        val uncertainVm = viewModel(uncertainApi)
        uncertainVm.start(selection)
        runCurrent()
        uncertainVm.dismiss()
        assertEquals(StartStatus.UNCERTAIN, uncertainVm.state.value.start)
    }

    @Test
    fun presentingAReportedProcessShowsItWithoutReadingItAgain() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = viewModel(api)
        val reported = process(state = "CONFIRMED")

        vm.present(reported)
        runCurrent()

        assertEquals(reported, vm.state.value.process)
        assertTrue(api.processes.isEmpty())
        assertEquals(0, api.pendings)
    }

    @Test
    fun dismissDoesNotCancelAnInFlightStart() = runTest(dispatcher) {
        val api = FakeApi()
        val response = CompletableDeferred<ReservationProcess>()
        api.onStart = { response.await() }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()
        vm.dismiss()
        assertEquals(StartStatus.SENDING, vm.state.value.start)

        response.complete(process())
        runCurrent()
        assertEquals(process(), vm.state.value.process)
    }

    @Test
    fun aFailedRefreshKeepsTheLastReportedProcess() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { process() }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()

        api.onProcess = { throw uncertain(READ_MESSAGE) }
        vm.refresh()
        runCurrent()

        val state = vm.state.value
        assertEquals(process(), state.process)
        assertEquals(READ_MESSAGE, state.message)
        assertFalse(state.loading)
    }

    @Test
    fun thePhoneRequirementAloneDoesNotEnableTheSubmission() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = false)
        }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()

        val current = assertNotNull(vm.state.value.process)
        assertEquals("PHONE_NUMBER", current.requiredField)
        assertFalse(vm.state.value.canSubmitPhone)

        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()
        assertEquals(0, api.phoneSubmissions.size)
    }

    @Test
    fun thePhoneFormOpensOnlyWhenTurnosEnablesTheSubmission() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()

        assertTrue(vm.state.value.canSubmitPhone)
        assertEquals("", vm.state.value.phoneNumber)
        assertEquals(PhoneStatus.IDLE, vm.state.value.phone)
    }

    @Test
    fun submittingThePhoneSendsItOnceNormalizedAndShowsTheReportedProcess() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ ->
            process(state = "PHONE_QUEUED", requiredField = "PHONE_NUMBER", canSubmitPhone = false)
        }
        val vm = viewModel(api, PHONE_REQUEST_ID)
        vm.discover()
        runCurrent()

        vm.changePhone("+54 9 11 5555-1234")
        vm.submitPhone()
        assertEquals(PhoneStatus.SENDING, vm.state.value.phone)
        assertFalse(vm.state.value.canSubmitPhone)
        runCurrent()

        val (processId, request) = api.phoneSubmissions.single()
        assertEquals(REQUEST_ID, processId)
        assertEquals(PHONE_REQUEST_ID, request.requestId)
        assertEquals("+5491155551234", request.phoneNumber)
        val state = vm.state.value
        assertEquals("PHONE_QUEUED", state.process?.state)
        assertEquals("", state.phoneNumber)
        assertEquals(PhoneStatus.IDLE, state.phone)
        assertFalse(state.canSubmitPhone)
    }

    @Test
    fun aSecondPhoneSubmitWhileTheRequestIsInFlightDoesNotSendAnotherOne() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        val pending = CompletableDeferred<ReservationProcess>()
        api.onPhone = { _, _ -> pending.await() }
        val vm = viewModel(api, PHONE_REQUEST_ID)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)

        vm.submitPhone()
        runCurrent()
        vm.submitPhone()
        runCurrent()
        assertEquals(1, api.phoneSubmissions.size)
        assertEquals(PhoneStatus.SENDING, vm.state.value.phone)

        pending.complete(process(state = "PHONE_QUEUED", canSubmitPhone = false))
        runCurrent()
        assertEquals("PHONE_QUEUED", vm.state.value.process?.state)
    }

    @Test
    fun anInvalidPhoneIsNotSentAndCanBeCorrected() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        val vm = viewModel(api, PHONE_REQUEST_ID)
        vm.discover()
        runCurrent()

        vm.changePhone("12ab")
        vm.submitPhone()
        runCurrent()
        assertEquals(0, api.phoneSubmissions.size)
        assertNotNull(vm.state.value.phoneMessage)
        assertTrue(vm.state.value.canSubmitPhone)

        vm.changePhone(PHONE)
        assertNull(vm.state.value.phoneMessage)
        vm.submitPhone()
        runCurrent()
        assertEquals(1, api.phoneSubmissions.size)
    }

    @Test
    fun aRejectedPhoneIsReconciledAndAllowsCorrection() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ ->
            throw rejected("El plazo para enviar el teléfono venció.", processId = REQUEST_ID)
        }
        api.onProcess = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        val vm = viewModel(api, PHONE_REQUEST_ID, PHONE_REQUEST_ID_2)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()

        assertEquals(listOf(REQUEST_ID), api.processes)
        assertEquals("El plazo para enviar el teléfono venció.", vm.state.value.phoneMessage)
        assertEquals(PhoneStatus.IDLE, vm.state.value.phone)
        assertTrue(vm.state.value.canSubmitPhone)

        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()
        assertEquals(
            listOf(PHONE_REQUEST_ID, PHONE_REQUEST_ID_2),
            api.phoneSubmissions.map { it.second.requestId },
        )
    }

    @Test
    fun anUncertainPhoneIsNotRepeatedUntilTheStateIsVerified() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ -> throw uncertain(PHONE_UNCERTAIN_MESSAGE) }
        val vm = viewModel(api, PHONE_REQUEST_ID)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()

        assertEquals(PhoneStatus.UNCERTAIN, vm.state.value.phone)
        assertEquals(1, api.phoneSubmissions.size)
        vm.submitPhone()
        runCurrent()
        assertEquals(1, api.phoneSubmissions.size)
        assertFalse(vm.state.value.canSubmitPhone)

        api.onProcess = { process(state = "PHONE_QUEUED", canSubmitPhone = false) }
        vm.verifyPhone()
        runCurrent()
        assertEquals(listOf(REQUEST_ID), api.processes)
        assertEquals("PHONE_QUEUED", vm.state.value.process?.state)
        assertEquals(PhoneStatus.IDLE, vm.state.value.phone)
        assertFalse(vm.state.value.canSubmitPhone)
    }

    @Test
    fun verifyingAnUnrecordedPhoneAllowsARetryWithANewRequestId() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ -> throw uncertain(PHONE_UNCERTAIN_MESSAGE) }
        val vm = viewModel(api, PHONE_REQUEST_ID, PHONE_REQUEST_ID_2)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()

        // El GET no encuentra un envío activo: Turnos sigue habilitando el formulario.
        api.onProcess = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        vm.verifyPhone()
        runCurrent()
        assertTrue(vm.state.value.canSubmitPhone)

        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()
        assertEquals(
            listOf(PHONE_REQUEST_ID, PHONE_REQUEST_ID_2),
            api.phoneSubmissions.map { it.second.requestId },
        )
    }

    @Test
    fun aNotFoundPhoneBlocksResendingUntilTheStateIsVerified() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ -> throw notFound() }
        val vm = viewModel(api, PHONE_REQUEST_ID, PHONE_REQUEST_ID_2)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()

        // Un 404 no acredita que el proceso ni el envío no existan.
        assertEquals(PhoneStatus.UNCERTAIN, vm.state.value.phone)
        assertEquals(1, api.phoneSubmissions.size)
        vm.submitPhone()
        runCurrent()
        assertEquals(1, api.phoneSubmissions.size)
        assertFalse(vm.state.value.canSubmitPhone)

        api.onProcess = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        vm.verifyPhone()
        runCurrent()
        assertEquals(listOf(REQUEST_ID), api.processes)
        assertTrue(vm.state.value.canSubmitPhone)

        api.onPhone = { _, _ -> process(state = "PHONE_QUEUED", canSubmitPhone = false) }
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()
        assertEquals(
            listOf(PHONE_REQUEST_ID, PHONE_REQUEST_ID_2),
            api.phoneSubmissions.map { it.second.requestId },
        )
    }

    @Test
    fun aRejectedPhoneWhoseReadFailsBlocksResendingUntilVerified() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ ->
            throw rejected("El plazo para enviar el teléfono venció.", processId = REQUEST_ID)
        }
        api.onProcess = { throw uncertain(READ_MESSAGE) }
        val vm = viewModel(api, PHONE_REQUEST_ID, PHONE_REQUEST_ID_2)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()

        // El rechazo no se confirma con el GET; el último estado ya puede estar vencido.
        assertEquals(listOf(REQUEST_ID), api.processes)
        assertEquals(PhoneStatus.UNCERTAIN, vm.state.value.phone)
        assertEquals(1, api.phoneSubmissions.size)
        vm.submitPhone()
        runCurrent()
        assertEquals(1, api.phoneSubmissions.size)
        assertFalse(vm.state.value.canSubmitPhone)

        api.onProcess = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        api.onPhone = { _, _ -> process(state = "PHONE_QUEUED", canSubmitPhone = false) }
        vm.verifyPhone()
        runCurrent()
        assertTrue(vm.state.value.canSubmitPhone)
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()
        assertEquals(2, api.phoneSubmissions.size)
        assertEquals(PHONE_REQUEST_ID_2, api.phoneSubmissions.last().second.requestId)
    }

    @Test
    fun aLatePhoneResponseAfterResetDoesNotPublishState() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = {
            process(state = "WAITING_FOR_PHONE", requiredField = "PHONE_NUMBER", canSubmitPhone = true)
        }
        val response = CompletableDeferred<ReservationProcess>()
        api.onPhone = { _, _ -> response.await() }
        val vm = viewModel(api, PHONE_REQUEST_ID)
        vm.discover()
        runCurrent()
        vm.changePhone(PHONE)
        vm.submitPhone()
        runCurrent()

        vm.reset()
        response.complete(process(state = "PHONE_QUEUED"))
        runCurrent()
        assertEquals(ReservationUiState(), vm.state.value)
    }

    @Test
    fun aKnownProcessSupersedesAFailedDiscoveryInTheBanner() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { throw uncertain(READ_MESSAGE) }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()
        assertEquals(BannerAction.RETRY_DISCOVERY, vm.state.value.banner?.action)

        api.onStart = { process() }
        vm.start(selection)
        runCurrent()
        vm.dismiss()

        // El proceso conocido es más específico que el fallo inicial de descubrimiento.
        assertEquals(BannerAction.SHOW_PROCESS, vm.state.value.banner?.action)
    }

    @Test
    fun aLateStartResponseAfterResetDoesNotPublishState() = runTest(dispatcher) {
        val api = FakeApi()
        val response = CompletableDeferred<ReservationProcess>()
        api.onStart = { response.await() }
        val vm = viewModel(api)

        vm.start(selection)
        runCurrent()
        vm.reset()
        response.complete(process())
        runCurrent()

        assertEquals(ReservationUiState(), vm.state.value)
    }

    @Test
    fun aLateRefreshResponseAfterResetDoesNotPublishState() = runTest(dispatcher) {
        val api = FakeApi()
        api.onPending = { process() }
        val vm = viewModel(api)
        vm.discover()
        runCurrent()

        val response = CompletableDeferred<ReservationProcess>()
        api.onProcess = { response.await() }
        vm.refresh()
        runCurrent()
        assertTrue(vm.state.value.loading)

        vm.reset()
        response.complete(process(state = "HELD"))
        runCurrent()

        assertEquals(ReservationUiState(), vm.state.value)
    }

    @Test
    fun aResponseFromAPreviousOperationDoesNotReplaceTheNewerOne() = runTest(dispatcher) {
        val api = FakeApi()
        val first = CompletableDeferred<ReservationProcess>()
        api.onStart = { first.await() }
        val vm = viewModel(api)
        vm.start(selection)
        runCurrent()

        vm.reset()
        val second = CompletableDeferred<ReservationProcess>()
        api.onStart = { second.await() }
        vm.start(selection)
        runCurrent()

        first.complete(process(state = "CONFIRMED"))
        runCurrent()
        assertEquals(StartStatus.SENDING, vm.state.value.start)
        assertNull(vm.state.value.process)

        second.complete(process(state = "HELD"))
        runCurrent()
        assertEquals("HELD", vm.state.value.process?.state)
    }

    private fun rejected(message: String, processId: String? = null) =
        ReservationException(ReservationFailure.REJECTED, processId, message)

    private fun uncertain(message: String) =
        ReservationException(ReservationFailure.UNCERTAIN, message = message)

    private fun notFound() =
        ReservationException(ReservationFailure.NOT_FOUND, message = "No encontramos esta solicitud.")

    private class FakeApi : ReservationsApi {
        val starts = mutableListOf<StartReservation>()
        val processes = mutableListOf<String>()
        val phoneSubmissions = mutableListOf<Pair<String, SubmitPhone>>()
        var pendings = 0

        var onStart: suspend (StartReservation) -> ReservationProcess = { sampleProcess(it.requestId) }
        var onProcess: suspend (String) -> ReservationProcess = { sampleProcess(it) }
        var onPending: suspend () -> ReservationProcess? = { null }
        var onPhone: suspend (String, SubmitPhone) -> ReservationProcess = { id, _ -> sampleProcess(id) }

        override suspend fun start(request: StartReservation): ReservationProcess {
            starts += request
            return onStart(request)
        }

        override suspend fun process(processId: String): ReservationProcess {
            processes += processId
            return onProcess(processId)
        }

        override suspend fun pending(): ReservationProcess? {
            pendings++
            return onPending()
        }

        override suspend fun reservations(
            page: Int,
            size: Int,
        ): OwnReservationsPage = error("not used")

        override suspend fun cancel(processId: String): ReservationProcess = error("not used")

        override suspend fun submitPhone(
            processId: String,
            request: SubmitPhone,
        ): ReservationProcess {
            phoneSubmissions += processId to request
            return onPhone(processId, request)
        }

        private companion object {
            fun sampleProcess(id: String) = ReservationProcess(
                id = id,
                professionalId = 201,
                date = "2026-11-09",
                startTime = "12:30:00",
                state = "HOLD_PENDING",
                expiresAt = "2026-11-09T15:30:00Z",
                reasonCode = null,
                requiredField = null,
                canSubmitPhone = false,
                professionalFirstName = "Ana",
                professionalLastName = "Torres",
            )
        }
    }

    private companion object {
        const val REQUEST_ID = "b7273764-9bc3-43fd-b42c-8a9ca65c5639"
        const val PHONE_REQUEST_ID = "11111111-1111-4111-8111-111111111111"
        const val PHONE_REQUEST_ID_2 = "22222222-2222-4222-8222-222222222222"
        const val PHONE = "+54 9 11 5555-1234"
        const val START_MESSAGE = "No pudimos confirmar si tu solicitud quedó registrada. Pudo haberse registrado."
        const val READ_MESSAGE = "No pudimos actualizar el estado de tu solicitud."
        const val PHONE_UNCERTAIN_MESSAGE =
            "No pudimos confirmar si recibimos tu teléfono. El envío pudo haberse registrado."
        const val ACTIVE_MESSAGE = "Ya tenés una solicitud de turno pendiente."
    }
}
