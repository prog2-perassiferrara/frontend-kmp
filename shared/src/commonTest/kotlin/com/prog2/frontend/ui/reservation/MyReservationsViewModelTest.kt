package com.prog2.frontend.ui.reservation

import androidx.lifecycle.ViewModelStore
import com.prog2.frontend.reservation.ReservationException
import com.prog2.frontend.reservation.ReservationFailure
import com.prog2.frontend.reservation.data.CancellationProgress
import com.prog2.frontend.reservation.data.OwnReservationsPage
import com.prog2.frontend.reservation.data.ReservationProcess
import com.prog2.frontend.reservation.data.ReservationsApi
import com.prog2.frontend.reservation.data.StartReservation
import com.prog2.frontend.reservation.data.SubmitPhone
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
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
class MyReservationsViewModelTest {
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

    private fun viewModel(api: ReservationsApi): MyReservationsViewModel =
        MyReservationsViewModel(api).also {
            owners += ViewModelStore().apply { put("reservations", it) }
        }

    @Test
    fun loadsTheFirstPageAndShowsAnEmptyList() = runTest(dispatcher) {
        val api = FakeApi()
        val response = CompletableDeferred<OwnReservationsPage>()
        api.onReservations = { _, _ -> response.await() }
        val vm = viewModel(api)

        vm.load()
        runCurrent()
        assertTrue(vm.state.value.loading)

        response.complete(page(emptyList(), 0, 10, total = 0))
        runCurrent()

        assertEquals(listOf(0), api.pages)
        assertEquals(10, api.sizes.single())
        assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.loaded)
        assertTrue(vm.state.value.noHistory)
        assertNull(vm.state.value.message)
    }

    @Test
    fun reopeningTheScreenReadsTheFirstPageAgain() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = viewModel(api)

        vm.load()
        runCurrent()
        vm.load()
        runCurrent()

        assertEquals(listOf(0, 0), api.pages)
    }

    @Test
    fun aFailedLoadShowsAnErrorAndRetryReadsAgain() = runTest(dispatcher) {
        val api = FakeApi()
        api.onReservations = { _, _ ->
            throw ReservationException(ReservationFailure.UNCERTAIN, message = "No pudimos consultar tus reservas.")
        }
        val vm = viewModel(api)

        vm.load()
        runCurrent()
        assertEquals("No pudimos consultar tus reservas.", vm.state.value.message)
        assertFalse(vm.state.value.noHistory)

        api.onReservations = { page, size -> page(listOf(confirmed("1")), page, size, total = 1) }
        vm.refresh()
        runCurrent()
        assertNull(vm.state.value.message)
        assertEquals(1, vm.state.value.reservations.size)
        assertEquals(listOf(0, 0), api.pages)
    }

    @Test
    fun paginatesWithinTheReportedTotals() = runTest(dispatcher) {
        val api = FakeApi()
        api.onReservations = { page, size ->
            page(listOf(confirmed("$page")), page, size, total = 21, totalPages = 3)
        }
        val vm = viewModel(api)

        vm.load()
        runCurrent()
        assertEquals(0, vm.state.value.page)
        assertFalse(vm.state.value.canGoBack)
        assertTrue(vm.state.value.canGoForward)

        vm.next()
        runCurrent()
        assertEquals(1, vm.state.value.page)
        assertTrue(vm.state.value.canGoBack)

        vm.next()
        runCurrent()
        assertEquals(2, vm.state.value.page)
        assertFalse(vm.state.value.canGoForward)

        vm.previous()
        runCurrent()
        assertEquals(1, vm.state.value.page)
        assertEquals(listOf(0, 1, 2, 1), api.pages)
    }

    @Test
    fun cancellationIsOnlyOfferedForConfirmedReservationsWithoutPendingCancellation() = runTest(dispatcher) {
        val api = FakeApi()
        val confirmedWithoutCancellation = confirmed("1")
        val pending = confirmed("2", cancellation = CancellationProgress("PENDING", canRetry = false))
        val retry = confirmed("3", cancellation = CancellationProgress("RETRY_REQUIRED", canRetry = true))
        val cancelled = confirmed("4").copy(state = "CANCELLED")
        api.onReservations = { page, size ->
            page(listOf(confirmedWithoutCancellation, pending, retry, cancelled), page, size, total = 4)
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        assertTrue(vm.state.value.canCancel(confirmedWithoutCancellation))
        assertFalse(vm.state.value.canCancel(pending))
        assertTrue(vm.state.value.canCancel(retry))
        assertFalse(vm.state.value.canCancel(cancelled))

        vm.openCancellation(pending)
        assertNull(vm.state.value.cancelTarget)
        vm.openCancellation(confirmedWithoutCancellation)
        assertNotNull(vm.state.value.cancelTarget)
    }

    @Test
    fun confirmingCancellationSendsOneCallAndUpdatesTheRow() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        api.onReservations = { page, size -> page(listOf(target), page, size, total = 1) }
        val response = CompletableDeferred<ReservationProcess>()
        api.onCancel = { response.await() }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()
        assertNull(vm.state.value.cancelTarget)
        assertEquals(PROCESS_ID, vm.state.value.cancellingId)

        // Una segunda pulsación durante el POST no envía otra solicitud ni reabre el diálogo.
        vm.confirmCancellation()
        vm.openCancellation(target)
        assertNull(vm.state.value.cancelTarget)
        assertEquals(listOf(PROCESS_ID), api.cancelled)

        response.complete(target.copy(state = "CANCELLED"))
        runCurrent()
        assertNull(vm.state.value.cancellingId)
        assertEquals("CANCELLED", vm.state.value.reservations.single().state)
        assertEquals("Tu turno fue cancelado.", vm.state.value.cancelMessage)
    }

    @Test
    fun aPendingCancellationIsShownWithoutDeclaringItCancelled() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        api.onReservations = { page, size -> page(listOf(target), page, size, total = 1) }
        api.onCancel = {
            target.copy(cancellation = CancellationProgress("PENDING", canRetry = false))
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()

        val row = vm.state.value.reservations.single()
        assertEquals("CONFIRMED", row.state)
        assertTrue(row.cancellationPending)
        assertTrue(vm.state.value.cancelMessage!!.contains("en curso"))
        assertFalse(vm.state.value.canCancel(row))
    }

    @Test
    fun aRejectedCancellationKeepsTheReservationAndRefreshesTheList() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        api.onReservations = { page, size -> page(listOf(target), page, size, total = 1) }
        api.onCancel = {
            throw ReservationException(ReservationFailure.REJECTED, message = "Esta reserva no se puede cancelar.")
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()

        assertEquals("CONFIRMED", vm.state.value.reservations.single().state)
        assertEquals("Esta reserva no se puede cancelar.", vm.state.value.cancelMessage)
        assertNull(vm.state.value.cancellingId)
        assertEquals(listOf(0, 0), api.pages)
    }

    @Test
    fun anUncertainCancellationBlocksUntilAReadVerifiesTheReservation() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        var reads = 0
        api.onReservations = { page, size ->
            reads++
            if (reads > 1) {
                throw ReservationException(
                    ReservationFailure.UNCERTAIN,
                    message = "No pudimos consultar tus reservas.",
                )
            }
            page(listOf(target), page, size, total = 1)
        }
        api.onCancel = {
            throw ReservationException(
                ReservationFailure.UNCERTAIN,
                message = "No pudimos confirmar si la cancelación se completó.",
            )
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()

        // La lectura posterior falló: la reserva sigue bloqueada y visible.
        assertEquals(PROCESS_ID, vm.state.value.cancelBlockedId)
        assertFalse(vm.state.value.canCancel(vm.state.value.reservations.single()))
        assertEquals("CONFIRMED", vm.state.value.reservations.single().state)
        assertEquals(listOf(0, 0), api.pages)

        api.onReservations = { page, size -> page(listOf(target), page, size, total = 1) }
        vm.refresh()
        runCurrent()
        assertNull(vm.state.value.cancelBlockedId)
        assertTrue(vm.state.value.canCancel(vm.state.value.reservations.single()))
    }

    @Test
    fun anUncertainCancellationThatReadsAPendingProgressIsNotOfferedAgain() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        var reads = 0
        api.onReservations = { page, size ->
            reads++
            val row = if (reads == 1) {
                target
            } else {
                target.copy(cancellation = CancellationProgress("PENDING", canRetry = false))
            }
            page(listOf(row), page, size, total = 1)
        }
        api.onCancel = {
            throw ReservationException(
                ReservationFailure.UNCERTAIN,
                message = "No pudimos confirmar si la cancelación se completó.",
            )
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()

        val row = vm.state.value.reservations.single()
        assertNull(vm.state.value.cancelBlockedId)
        assertTrue(row.cancellationPending)
        assertFalse(vm.state.value.canCancel(row))
        assertEquals(listOf(0, 0), api.pages)
    }

    @Test
    fun aRejectedCancellationWhoseReadFailsBlocksUntilVerified() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        var reads = 0
        api.onReservations = { page, size ->
            reads++
            if (reads > 1) {
                throw ReservationException(
                    ReservationFailure.UNCERTAIN,
                    message = "No pudimos consultar tus reservas.",
                )
            }
            page(listOf(target), page, size, total = 1)
        }
        api.onCancel = {
            throw ReservationException(
                ReservationFailure.REJECTED,
                message = "Esta reserva no se puede cancelar.",
            )
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()

        // El rechazo llegó, pero la reconciliación falló: la fila anterior no se ofrece de nuevo.
        assertEquals(PROCESS_ID, vm.state.value.cancelBlockedId)
        assertFalse(vm.state.value.canCancel(vm.state.value.reservations.single()))
        assertEquals("CONFIRMED", vm.state.value.reservations.single().state)
        assertEquals("Esta reserva no se puede cancelar.", vm.state.value.cancelMessage)
        assertEquals(listOf(0, 0), api.pages)

        api.onReservations = { page, size -> page(listOf(target), page, size, total = 1) }
        vm.refresh()
        runCurrent()
        assertNull(vm.state.value.cancelBlockedId)
        assertTrue(vm.state.value.canCancel(vm.state.value.reservations.single()))
    }

    @Test
    fun aRejectedCancellationWhoseReadLosesTheSessionKeepsTheBlockWithoutEscaping() = runTest(dispatcher) {
        val api = FakeApi()
        val target = confirmed(PROCESS_ID)
        var reads = 0
        api.onReservations = { page, size ->
            reads++
            if (reads > 1) throw SessionException(SessionError.SESSION_REQUIRED)
            page(listOf(target), page, size, total = 1)
        }
        api.onCancel = {
            throw ReservationException(
                ReservationFailure.REJECTED,
                message = "Esta reserva no se puede cancelar.",
            )
        }
        val vm = viewModel(api)
        vm.load()
        runCurrent()

        vm.openCancellation(target)
        vm.confirmCancellation()
        runCurrent()

        // La sesión se perdió al reconciliar: no escapa una excepción y la fila
        // anterior sigue bloqueada hasta que App descarte el estado.
        assertEquals(PROCESS_ID, vm.state.value.cancelBlockedId)
        assertNull(vm.state.value.cancellingId)
        assertEquals("Esta reserva no se puede cancelar.", vm.state.value.cancelMessage)
        assertEquals("CONFIRMED", vm.state.value.reservations.single().state)
        assertFalse(vm.state.value.canCancel(vm.state.value.reservations.single()))
    }

    @Test
    fun resetClearsTheStateAndAllowsANewRead() = runTest(dispatcher) {
        val api = FakeApi()
        api.onReservations = { page, size -> page(listOf(confirmed(PROCESS_ID)), page, size, total = 1) }
        val vm = viewModel(api)
        vm.load()
        runCurrent()
        assertEquals(1, vm.state.value.reservations.size)

        vm.reset()
        assertEquals(MyReservationsUiState(), vm.state.value)

        vm.load()
        runCurrent()
        assertEquals(listOf(0, 0), api.pages)
    }

    private fun confirmed(
        id: String,
        cancellation: CancellationProgress? = null,
    ) = ReservationProcess(
        id = id,
        professionalId = 201,
        date = "2026-11-09",
        startTime = "12:30:00",
        state = "CONFIRMED",
        expiresAt = "2026-11-09T15:30:00Z",
        reasonCode = null,
        requiredField = null,
        canSubmitPhone = false,
        professionalFirstName = "Ana",
        professionalLastName = "Torres",
        cancellation = cancellation,
    )

    private fun page(
        content: List<ReservationProcess>,
        page: Int,
        size: Int,
        total: Long,
        totalPages: Int = if (total == 0L) 0 else ((total + size - 1) / size).toInt(),
    ) = OwnReservationsPage(
        pending = null,
        content = content,
        page = page,
        size = size,
        totalElements = total,
        totalPages = totalPages,
    )

    private class FakeApi : ReservationsApi {
        val pages = mutableListOf<Int>()
        val sizes = mutableListOf<Int>()
        val cancelled = mutableListOf<String>()

        var onReservations: suspend (Int, Int) -> OwnReservationsPage = { page, size ->
            OwnReservationsPage(null, emptyList(), page, size, 0, 0)
        }
        var onCancel: suspend (String) -> ReservationProcess = { error("not configured") }

        override suspend fun start(request: StartReservation): ReservationProcess = error("not used")

        override suspend fun process(processId: String): ReservationProcess = error("not used")

        override suspend fun pending(): ReservationProcess? = error("not used")

        override suspend fun submitPhone(
            processId: String,
            request: SubmitPhone,
        ): ReservationProcess = error("not used")

        override suspend fun reservations(page: Int, size: Int): OwnReservationsPage {
            pages += page
            sizes += size
            return onReservations(page, size)
        }

        override suspend fun cancel(processId: String): ReservationProcess {
            cancelled += processId
            return onCancel(processId)
        }
    }

    private companion object {
        const val PROCESS_ID = "b7273764-9bc3-43fd-b42c-8a9ca65c5639"
    }
}
