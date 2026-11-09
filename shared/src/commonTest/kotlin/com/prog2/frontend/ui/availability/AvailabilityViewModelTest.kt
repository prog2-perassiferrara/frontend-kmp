package com.prog2.frontend.ui.availability

import androidx.lifecycle.ViewModelStore
import com.prog2.frontend.availability.AvailabilityError
import com.prog2.frontend.availability.AvailabilityException
import com.prog2.frontend.availability.data.AvailabilityApi
import com.prog2.frontend.availability.data.ProfessionalAvailability
import com.prog2.frontend.catalog.data.CatalogCategory
import com.prog2.frontend.catalog.data.Professional
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
class AvailabilityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<ViewModelStore>()

    private val ana = Professional(201, "Ana", "Torres", true, CatalogCategory(101, "Cardiologia", true))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        owners.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun viewModel(api: AvailabilityApi): AvailabilityViewModel = AvailabilityViewModel(api).also {
        owners += ViewModelStore().apply { put("availability", it) }
    }

    private fun availabilityOf(
        professionalId: Long = 201,
        date: String = "2026-11-09",
        times: List<String> = listOf("12:00:00"),
    ) = ProfessionalAvailability(professionalId, date, "America/Argentina/Buenos_Aires", times)

    @Test
    fun openShowsTheProfessionalAndKeepsTheSearchDate() {
        val vm = viewModel(FakeApi())

        vm.open(ana, "2026-11-09")

        assertEquals(201, vm.state.value.professionalId)
        assertEquals("Ana Torres", vm.state.value.professionalName)
        assertEquals("Cardiologia", vm.state.value.categoryName)
        assertEquals("2026-11-09", vm.state.value.date)
        assertFalse(vm.state.value.hasResult)
        assertEquals(emptyList(), vm.state.value.times)
    }

    @Test
    fun aDateWithoutIsoShapeIsNotPrefilledFromTheSearch() {
        val vm = viewModel(FakeApi())

        vm.open(ana, "09/11/2026")

        assertEquals("", vm.state.value.date)
    }

    @Test
    fun consultPublishesTheTimesInTheReceivedOrder() = runTest(dispatcher) {
        val api = FakeApi()
        api.answer = { id, date -> availabilityOf(id, date, listOf("12:00:00", "12:30:00", "13:30:00")) }
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")

        vm.consult()
        runCurrent()

        assertEquals(listOf(201L to "2026-11-09"), api.calls)
        assertEquals(listOf("12:00:00", "12:30:00", "13:30:00"), vm.state.value.times)
        assertTrue(vm.state.value.hasResult)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun anEmptyAnswerIsAValidResultWithoutError() = runTest(dispatcher) {
        val api = FakeApi()
        api.answer = { id, date -> availabilityOf(id, date, emptyList()) }
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")

        vm.consult()
        runCurrent()

        assertTrue(vm.state.value.hasResult)
        assertTrue(vm.state.value.isEmptyResult)
        assertNull(vm.state.value.error)
    }

    @Test
    fun aDateWithoutIsoShapeBlocksTheRequest() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")
        vm.changeDate("09/11/2026")

        vm.consult()
        runCurrent()

        assertTrue(api.calls.isEmpty())
        assertNotNull(vm.state.value.dateError)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun unavailableOffersRetryAndRejectedCriteriaDoesNot() = runTest(dispatcher) {
        val api = FakeApi()
        api.answer = { _, _ -> throw AvailabilityException(AvailabilityError.UNAVAILABLE) }
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")

        vm.consult()
        runCurrent()
        assertEquals(AvailabilityError.UNAVAILABLE.message, vm.state.value.error)
        assertTrue(vm.state.value.retryable)

        api.answer = { id, date -> availabilityOf(id, date) }
        vm.retry()
        runCurrent()
        assertEquals(2, api.calls.size)
        assertEquals(listOf("12:00:00"), vm.state.value.times)
        assertNull(vm.state.value.error)

        api.answer = { _, _ -> throw AvailabilityException(AvailabilityError.INVALID_QUERY) }
        vm.consult()
        runCurrent()
        assertEquals(AvailabilityError.INVALID_QUERY.message, vm.state.value.error)
        assertFalse(vm.state.value.retryable)
    }

    @Test
    fun changingTheDateDiscardsPreviousResultsAndIgnoresThePendingAnswer() = runTest(dispatcher) {
        val api = FakeApi()
        val gate = CompletableDeferred<Unit>()
        api.gate = gate
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")

        vm.consult()
        runCurrent()
        assertEquals(1, api.calls.size)

        vm.changeDate("2026-11-10")
        gate.complete(Unit)
        runCurrent()

        assertFalse(vm.state.value.hasResult)
        assertTrue(vm.state.value.times.isEmpty())
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)

        api.gate = null
        vm.consult()
        runCurrent()
        assertEquals(201L to "2026-11-10", api.calls.last())
        assertEquals(listOf("12:00:00"), vm.state.value.times)
    }

    @Test
    fun openKeepsTheConsultedStateForTheSameProfessional() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")
        vm.consult()
        runCurrent()

        // La rotación vuelve a componer la pantalla con el mismo profesional.
        vm.open(ana, "")

        assertEquals("2026-11-09", vm.state.value.date)
        assertEquals(listOf("12:00:00"), vm.state.value.times)
        assertTrue(vm.state.value.hasResult)
    }

    @Test
    fun selectingAnOfferedTimeMarksItAndDiscardingTheDateClearsIt() = runTest(dispatcher) {
        val api = FakeApi()
        api.answer = { id, date -> availabilityOf(id, date, listOf("12:00:00", "12:30:00")) }
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")
        vm.consult()
        runCurrent()

        vm.selectTime("12:30:00")
        assertEquals("12:30:00", vm.state.value.selectedTime)

        // Un horario que Turnos no ofreció no reemplaza la marca vigente.
        vm.selectTime("13:00:00")
        assertEquals("12:30:00", vm.state.value.selectedTime)

        // La rotación recompone con el mismo profesional y conserva la marca.
        vm.open(ana, "")
        assertEquals("12:30:00", vm.state.value.selectedTime)

        vm.changeDate("2026-11-10")
        assertNull(vm.state.value.selectedTime)
    }

    @Test
    fun resetDiscardsTheSelectionAndThePendingAnswer() = runTest(dispatcher) {
        val api = FakeApi()
        val gate = CompletableDeferred<Unit>()
        api.gate = gate
        val vm = viewModel(api)
        vm.open(ana, "2026-11-09")
        vm.consult()
        runCurrent()

        vm.reset()
        gate.complete(Unit)
        runCurrent()

        assertEquals(AvailabilityUiState(), vm.state.value)
    }

    private inner class FakeApi : AvailabilityApi {
        var answer: (Long, String) -> ProfessionalAvailability = { id, date -> availabilityOf(id, date) }
        var gate: CompletableDeferred<Unit>? = null
        val calls = mutableListOf<Pair<Long, String>>()

        override suspend fun availability(professionalId: Long, date: String): ProfessionalAvailability {
            calls += professionalId to date
            gate?.await()
            return answer(professionalId, date)
        }
    }
}
