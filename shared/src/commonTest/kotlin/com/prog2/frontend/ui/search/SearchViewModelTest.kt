package com.prog2.frontend.ui.search

import androidx.lifecycle.ViewModelStore
import com.prog2.frontend.catalog.CatalogError
import com.prog2.frontend.catalog.CatalogException
import com.prog2.frontend.catalog.data.CatalogApi
import com.prog2.frontend.catalog.data.CatalogCategory
import com.prog2.frontend.catalog.data.CatalogState
import com.prog2.frontend.catalog.data.Professional
import com.prog2.frontend.catalog.data.ProfessionalPage
import com.prog2.frontend.catalog.data.ProfessionalQuery
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
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<ViewModelStore>()

    private val ana = Professional(15, "Ana", "Gómez", true, CatalogCategory(1, "Clínica", true))
    private val clinic = CatalogCategory(1, "Clínica", true)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        owners.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun viewModel(api: CatalogApi): SearchViewModel = SearchViewModel(api).also {
        owners += ViewModelStore().apply { put("search", it) }
    }

    private fun page(
        content: List<Professional> = emptyList(),
        page: Int = 0,
        totalPages: Int = if (content.isEmpty()) 0 else 1,
        totalElements: Long = content.size.toLong(),
        state: String = "CURRENT",
        copyAvailable: Boolean = true,
    ) = ProfessionalPage(content, page, 20, totalElements, totalPages, CatalogState(state, copyAvailable))

    @Test
    fun initialLoadSearchesWithoutFiltersAndFlagsOutdatedCopy() = runTest(dispatcher) {
        val api = FakeCatalogApi(categoriesAnswer = { listOf(clinic) })
        api.searchAnswer = { page(listOf(ana), state = "STALE") }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()

        assertEquals(listOf(ProfessionalQuery()), api.queries)
        assertEquals(listOf(clinic), vm.state.value.categories)
        assertEquals(listOf(ana), vm.state.value.results)
        assertEquals(1, vm.state.value.totalElements)
        assertTrue(vm.state.value.hasResult)
        assertTrue(vm.state.value.catalogOutdated)
        assertNull(vm.state.value.error)
    }

    @Test
    fun combinesTheFourFiltersAndRestartsAtTheFirstPage() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.searchAnswer = { query -> page(listOf(ana), page = query.page, totalPages = 3, totalElements = 3) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()
        vm.nextPage()
        runCurrent()
        assertEquals(1, vm.state.value.page)

        vm.changeName("  Ana  ")
        vm.changeCategory(1)
        vm.changeEnabled(false)
        vm.changeAvailableOn("2026-11-09")
        vm.search()
        runCurrent()

        assertEquals(
            ProfessionalQuery(
                categoryId = 1,
                name = "Ana",
                enabled = false,
                availableOn = "2026-11-09",
                page = 0,
            ),
            api.queries.last(),
        )
        assertEquals(0, vm.state.value.page)
    }

    @Test
    fun invalidNameLengthOrDateShapeBlocksTheRequest() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()
        assertEquals(1, api.queries.size)

        vm.changeName("x".repeat(101))
        vm.search()
        runCurrent()
        assertEquals(1, api.queries.size)
        assertNotNull(vm.state.value.nameError)

        vm.changeName("Ana")
        vm.changeAvailableOn("09/11/2026")
        vm.search()
        runCurrent()
        assertEquals(1, api.queries.size)
        assertNotNull(vm.state.value.availableOnError)
    }

    @Test
    fun anEmptyPageIsAValidResultWithoutError() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()

        assertTrue(vm.state.value.isEmptyResult)
        assertEquals(0, vm.state.value.totalPages)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.catalogOutdated)
    }

    @Test
    fun unavailableCopyIsAnErrorAndRetryRepeatsTheQuery() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.searchAnswer = { throw CatalogException(CatalogError.COPY_UNAVAILABLE) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()

        assertEquals(CatalogError.COPY_UNAVAILABLE.message, vm.state.value.error)
        assertTrue(vm.state.value.retryable)
        assertFalse(vm.state.value.hasResult)
        assertFalse(vm.state.value.isEmptyResult)

        api.searchAnswer = { page(listOf(ana)) }
        vm.retry()
        runCurrent()

        assertEquals(2, api.queries.size)
        assertEquals(listOf(ana), vm.state.value.results)
        assertNull(vm.state.value.error)
    }

    @Test
    fun invalidCriteriaAsksForCorrectionInsteadOfBlindRetry() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.searchAnswer = { throw CatalogException(CatalogError.INVALID_CRITERIA) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()

        assertEquals(CatalogError.INVALID_CRITERIA.message, vm.state.value.error)
        assertFalse(vm.state.value.retryable)
    }

    @Test
    fun aCategoriesFailureDoesNotBlockSearchAndTheNextSearchReloadsThem() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.categoriesAnswer = { throw CatalogException(CatalogError.UNAVAILABLE) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()

        assertTrue(vm.state.value.categoriesUnavailable)
        assertEquals(1, api.queries.size)

        api.categoriesAnswer = { listOf(clinic) }
        vm.search()
        runCurrent()

        assertEquals(2, api.categoryCalls)
        assertFalse(vm.state.value.categoriesUnavailable)
        assertEquals(listOf(clinic), vm.state.value.categories)
    }

    @Test
    fun paginationRequestsAdjacentPagesOnly() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.searchAnswer = { query -> page(listOf(ana), page = query.page, totalPages = 2, totalElements = 2) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()

        vm.nextPage()
        runCurrent()
        assertEquals(1, api.queries.last().page)
        assertFalse(vm.state.value.canGoForward)
        assertTrue(vm.state.value.canGoBack)

        vm.nextPage()
        runCurrent()
        assertEquals(2, api.queries.size)

        vm.previousPage()
        runCurrent()
        assertEquals(0, api.queries.last().page)
        assertFalse(vm.state.value.canGoBack)
    }

    @Test
    fun repeatedSearchWhileLoadingSendsOneRequest() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        val gate = CompletableDeferred<Unit>()
        api.gate = gate
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()
        assertEquals(1, api.queries.size)

        vm.search()
        vm.nextPage()
        runCurrent()
        assertEquals(1, api.queries.size)

        gate.complete(Unit)
        runCurrent()
        assertEquals(1, api.queries.size)
        assertTrue(vm.state.value.hasResult)
    }

    @Test
    fun resetDiscardsFiltersResultsAndAcceptsANewInitialLoad() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.searchAnswer = { page(listOf(ana)) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()
        vm.changeName("Ana")

        vm.reset()

        assertEquals(SearchUiState(), vm.state.value)
        assertNull(vm.state.value.error)

        vm.ensureLoaded()
        runCurrent()
        assertEquals(2, api.queries.size)
        assertEquals(1, vm.state.value.results.size)
    }

    @Test
    fun editingAFilterInvalidatesThePreviousPaginationUntilTheNextSearch() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        api.searchAnswer = { query -> page(listOf(ana), page = query.page, totalPages = 3, totalElements = 3) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()
        vm.nextPage()
        runCurrent()
        assertEquals(1, vm.state.value.page)
        val queriesBeforeEdit = api.queries.size

        vm.changeCategory(1)
        assertFalse(vm.state.value.canGoForward)
        assertFalse(vm.state.value.canGoBack)
        vm.nextPage()
        vm.previousPage()
        runCurrent()
        assertEquals(queriesBeforeEdit, api.queries.size)

        vm.search()
        runCurrent()
        assertFalse(vm.state.value.paginationStale)
        assertEquals(0, vm.state.value.page)
        assertEquals(0, api.queries.last().page)
    }

    @Test
    fun resetCancelsAPendingCategoriesLoad() = runTest(dispatcher) {
        val api = FakeCatalogApi()
        val categoriesGate = CompletableDeferred<Unit>()
        api.categoriesGate = categoriesGate
        api.searchAnswer = { page(listOf(ana)) }
        val vm = viewModel(api)
        vm.ensureLoaded()
        runCurrent()
        assertEquals(1, api.categoryCalls)

        vm.reset()
        categoriesGate.complete(Unit)
        runCurrent()

        assertEquals(SearchUiState(), vm.state.value)
        assertFalse(vm.state.value.categoriesUnavailable)
        assertTrue(vm.state.value.categories.isEmpty())
    }

    private inner class FakeCatalogApi(
        var categoriesAnswer: () -> List<CatalogCategory> = { emptyList() },
    ) : CatalogApi {
        var searchAnswer: (ProfessionalQuery) -> ProfessionalPage = { page(emptyList()) }
        var gate: CompletableDeferred<Unit>? = null
        var categoriesGate: CompletableDeferred<Unit>? = null
        val queries = mutableListOf<ProfessionalQuery>()
        var categoryCalls = 0

        override suspend fun searchProfessionals(query: ProfessionalQuery): ProfessionalPage {
            queries += query
            gate?.await()
            return searchAnswer(query)
        }

        override suspend fun categories(): List<CatalogCategory> {
            categoryCalls++
            categoriesGate?.await()
            return categoriesAnswer()
        }
    }
}
