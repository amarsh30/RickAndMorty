package com.amr.feature.location

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Location
import com.amr.core.domain.usecase.FetchRelatedCharactersUseCase
import com.amr.core.domain.usecase.LoadNextLocationsPageUseCase
import com.amr.core.domain.usecase.ObserveLocationDetailUseCase
import com.amr.core.domain.usecase.ObserveLocationPaginationStateUseCase
import com.amr.core.domain.usecase.ObserveLocationResidentIdsUseCase
import com.amr.core.domain.usecase.ObserveLocationsUseCase
import com.amr.core.domain.usecase.ObserveRelatedCharactersUseCase
import com.amr.core.domain.usecase.RefreshLocationDetailUseCase
import com.amr.core.domain.usecase.RefreshLocationsUseCase
import com.amr.feature.location.detail.LocationDetailViewModel
import com.amr.feature.location.detail.LocationDetailViewModel.Companion.RESIDENT_PAGE_SIZE
import com.amr.feature.location.fake.FakeLocationRepository
import com.amr.feature.location.fake.FakeRelatedCharacterRepository
import com.amr.feature.location.presentation.LocationListViewModel
import com.amr.feature.location.presentation.STALE_DATA_MESSAGE
import kotlinx.coroutines.CompletableDeferred
import androidx.lifecycle.ViewModelStore
import com.amr.feature.location.presentation.DATABASE_ERROR_MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class LocationViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var locations: FakeLocationRepository
    private lateinit var related: FakeRelatedCharacterRepository

    private val earth = Location(1, "Earth (C-137)", "Planet", "Dimension C-137")
    private val citadel = Location(3, "Citadel of Ricks", "Space station", "unknown")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locations = FakeLocationRepository()
        related = FakeRelatedCharacterRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun listViewModel() = LocationListViewModel(
        ObserveLocationsUseCase(locations),
        ObserveLocationPaginationStateUseCase(locations),
        RefreshLocationsUseCase(locations),
        LoadNextLocationsPageUseCase(locations),
        com.amr.feature.location.fake.connectivityRestored(connectivity)
    )

    private val connectivity = com.amr.feature.location.fake.FakeConnectivityObserver()

    private fun kotlinx.coroutines.test.TestScope.reconnect() {
        connectivity.status.value = com.amr.feature.location.fake.OFFLINE
        advanceUntilIdle()
        connectivity.status.value = com.amr.feature.location.fake.ONLINE
        advanceUntilIdle()
    }

    private fun detailViewModel(id: Int = 3) = LocationDetailViewModel(
        id,
        ObserveLocationDetailUseCase(locations),
        ObserveLocationResidentIdsUseCase(locations),
        RefreshLocationDetailUseCase(locations),
        ObserveRelatedCharactersUseCase(related),
        FetchRelatedCharactersUseCase(related),
        com.amr.feature.location.fake.connectivityRestored(connectivity)
    )

    // --- connectivity recovery ---

    @Test
    fun list_offlineRefresh_isRetriedOnReconnect_whileVisible() = runTest(dispatcher) {
        locations.refreshError = DataError.NoConnection()
        val viewModel = listViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        locations.refreshError = null
        locations.refreshResult = listOf(earth)
        reconnect()

        assertEquals(2, locations.refreshCalls)
        assertEquals(listOf(earth), viewModel.uiState.value.locations)
    }

    @Test
    fun list_tabSwitchWhileOffline_recoversWhenReturningOnline() = runTest(dispatcher) {
        connectivity.status.value = com.amr.feature.location.fake.OFFLINE
        locations.refreshError = DataError.NoConnection()
        val viewModel = listViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        viewModel.setVisible(false)
        locations.refreshError = null
        locations.refreshResult = listOf(earth)
        connectivity.status.value = com.amr.feature.location.fake.ONLINE
        advanceUntilIdle()
        assertEquals(1, locations.refreshCalls)

        viewModel.setVisible(true)
        advanceUntilIdle()

        assertEquals(2, locations.refreshCalls)
        assertEquals(listOf(earth), viewModel.uiState.value.locations)
    }

    @Test
    fun detail_returningWithFailedResidentWindow_retriesThatWindowOnly() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to (1..30).toList())
        related.fetchError = DataError.NoConnection()
        val viewModel = detailViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        viewModel.setVisible(false)
        related.fetchError = null
        viewModel.setVisible(true)
        advanceUntilIdle()

        assertEquals(listOf((1..20).toList(), (1..20).toList()), related.fetchCalls)
        assertNull(viewModel.uiState.value.residentsErrorMessage)
    }

    @Test
    fun list_serverError_isNotRetriedOnReconnect() = runTest(dispatcher) {
        locations.refreshError = DataError.Server()
        val viewModel = listViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        reconnect()

        assertEquals(1, locations.refreshCalls)
    }

    @Test
    fun detail_offlineResidentWindow_isRetriedWithoutWidening() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to (1..30).toList())
        related.fetchError = DataError.NoConnection()
        val viewModel = detailViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        related.fetchError = null
        reconnect()

        assertEquals(listOf((1..20).toList(), (1..20).toList()), related.fetchCalls)
        assertEquals(20, viewModel.uiState.value.residents.size)
        assertNull(viewModel.uiState.value.residentsErrorMessage)
    }

    // --- list ---

    @Test
    fun list_loadsFirstPageOnOpen() = runTest(dispatcher) {
        locations.refreshResult = listOf(earth)

        val viewModel = listViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(earth), state.locations)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.canLoadMore)
        assertNull(state.errorMessage)
    }

    @Test
    fun list_offlineWithoutCache_showsReasonAndRetryWorks() = runTest(dispatcher) {
        locations.refreshError = DataError.NoConnection()
        val viewModel = listViewModel()
        advanceUntilIdle()
        assertEquals("Gagal memuat lokasi. Periksa koneksi internet.", viewModel.uiState.value.errorMessage)

        locations.refreshError = null
        locations.refreshResult = listOf(earth)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(earth), viewModel.uiState.value.locations)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun list_offlineWithCache_keepsListWithStaleWarning() = runTest(dispatcher) {
        locations.list.value = listOf(earth)
        locations.refreshError = DataError.NoConnection()

        val viewModel = listViewModel()
        advanceUntilIdle()

        assertEquals(listOf(earth), viewModel.uiState.value.locations)
        assertEquals(STALE_DATA_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun list_emptyResult_isNotAnError() = runTest(dispatcher) {
        locations.refreshResult = emptyList()

        val viewModel = listViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.locations.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun list_loadMore_appendsOnce_andIsIgnoredWhileRefreshing() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        locations.refreshGate = gate
        locations.refreshResult = listOf(earth)
        locations.nextPage = listOf(citadel)
        val viewModel = listViewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(0, locations.loadMoreCalls)

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(1, locations.loadMoreCalls)
        assertEquals(listOf(earth, citadel), viewModel.uiState.value.locations)
        assertTrue(viewModel.uiState.value.isEndReached)
    }

    @Test
    fun list_loadMoreFailure_isShownInFooterOnly() = runTest(dispatcher) {
        locations.refreshResult = listOf(earth)
        locations.loadMoreError = DataError.Timeout()
        val viewModel = listViewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.loadMoreErrorMessage)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(earth), viewModel.uiState.value.locations)
    }

    // --- detail ---

    @Test
    fun detail_offlineWithoutCache_showsError() = runTest(dispatcher) {
        locations.detailError = DataError.NoConnection()

        val viewModel = detailViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.location)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun detail_fetchesOnlyTheFirstResidentWindow() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to (1..101).toList())

        val viewModel = detailViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(citadel, state.location)
        assertEquals(101, state.residentCount)
        assertEquals((1..RESIDENT_PAGE_SIZE).toList(), related.fetchCalls.single())
        assertEquals(RESIDENT_PAGE_SIZE, state.residents.size)
        assertTrue(state.canShowMoreResidents)
    }

    // --- One shared collection of the resident-id query (6C.3A) ---

    @Test
    fun detail_residentIdQuery_isCollectedOnce_acrossObserverFetcherRetryAndWindowGrowth() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to (1..50).toList())
        related.fetchError = DataError.Timeout()
        val viewModel = detailViewModel()
        advanceUntilIdle()

        // Observer and fetcher share one subscription (was two, plus one per retry).
        assertEquals(1, locations.residentIdSubscriptions)
        assertEquals(1, locations.residentIds.subscriptionCount.value)
        assertNotNull(viewModel.uiState.value.residentsErrorMessage)

        related.fetchError = null
        viewModel.loadMoreResidents() // retry the failed window
        advanceUntilIdle()
        viewModel.loadMoreResidents() // grow the window
        advanceUntilIdle()

        assertEquals(1, locations.residentIdSubscriptions)
        assertEquals(listOf((1..20).toList(), (1..20).toList(), (1..40).toList()), related.fetchCalls)
        assertEquals(40, viewModel.uiState.value.residents.size)
        assertEquals(50, viewModel.uiState.value.residentCount)
        assertTrue(viewModel.uiState.value.canShowMoreResidents)
    }

    @Test
    fun detail_residentIdUpdates_stillReachStateAndFetch() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to listOf(1, 2))
        val viewModel = detailViewModel()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.residentCount)

        locations.residentIds.value = mapOf(3 to listOf(1, 2, 3))
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.residentCount)
        assertEquals(listOf(1, 2, 3), viewModel.uiState.value.residents.map { it.id })
        assertEquals(listOf(1, 2, 3), related.fetchCalls.last())
        assertEquals(1, locations.residentIdSubscriptions)
    }

    @Test
    fun detail_residentIdQueryFailure_showsDatabaseError_andKeepsTheLocation() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIdsError = IllegalStateException("db")
        val viewModel = detailViewModel()
        advanceUntilIdle()

        assertEquals(DATABASE_ERROR_MESSAGE, viewModel.uiState.value.residentsErrorMessage)
        assertEquals(citadel, viewModel.uiState.value.location)
        assertTrue(related.fetchCalls.isEmpty())
    }

    @Test
    fun detail_clearingTheViewModel_cancelsTheSharedCollection() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to listOf(1))
        val store = ViewModelStore()
        store.put("detail", detailViewModel())
        advanceUntilIdle()
        assertEquals(1, locations.residentIds.subscriptionCount.value)

        store.clear()
        advanceUntilIdle()

        assertEquals(0, locations.residentIds.subscriptionCount.value)
    }

    @Test
    fun detail_loadMoreResidents_widensWindowUntilAllShown() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to (1..30).toList())
        val viewModel = detailViewModel()
        advanceUntilIdle()

        viewModel.loadMoreResidents()
        advanceUntilIdle()
        viewModel.loadMoreResidents()
        advanceUntilIdle()

        assertEquals(2, related.fetchCalls.size)
        assertEquals((1..30).toList(), related.fetchCalls.last())
        assertEquals(30, viewModel.uiState.value.residents.size)
        assertFalse(viewModel.uiState.value.canShowMoreResidents)
    }

    @Test
    fun detail_residentFetchFailure_isRetriedByLoadMore() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)
        locations.residentIds.value = mapOf(3 to listOf(1, 2))
        related.fetchError = DataError.NoConnection()
        val viewModel = detailViewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.residentsErrorMessage)

        related.fetchError = null
        viewModel.loadMoreResidents()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.residentsErrorMessage)
        assertEquals(listOf(1, 2), viewModel.uiState.value.residents.map { it.id })
        assertEquals(listOf(listOf(1, 2), listOf(1, 2)), related.fetchCalls)
    }

    @Test
    fun detail_withoutResidents_makesNoCharacterRequest() = runTest(dispatcher) {
        locations.details.value = mapOf(3 to citadel)

        val viewModel = detailViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.residentCount)
        assertTrue(related.fetchCalls.isEmpty())
    }
}
