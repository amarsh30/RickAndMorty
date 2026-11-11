package com.amr.feature.character.presentation

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.ObservePaginationStateUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.fake.FakeCharacterRepository
import com.amr.feature.character.fake.FakeConnectivityObserver
import com.amr.feature.character.fake.OFFLINE
import com.amr.feature.character.fake.ONLINE
import com.amr.feature.character.fake.connectivityRestored
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Automatic recovery of the character list when the network returns. */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterRecoveryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCharacterRepository
    private lateinit var connectivity: FakeConnectivityObserver

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", null)
    private val morty = Character(2, "Morty Smith", "Alive", "Human", null)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCharacterRepository()
        connectivity = FakeConnectivityObserver(ONLINE)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CharacterViewModel(
        observeCharactersUseCase = ObserveCharactersUseCase(repository),
        refreshCharactersUseCase = RefreshCharactersUseCase(repository),
        observePaginationStateUseCase = ObservePaginationStateUseCase(repository),
        loadNextPageCharactersUseCase = LoadNextPageCharactersUseCase(repository),
        connectivityRestored = connectivityRestored(connectivity)
    )

    private fun TestScope.reconnect() {
        connectivity.status.value = OFFLINE
        advanceUntilIdle()
        connectivity.status.value = ONLINE
        advanceUntilIdle()
    }

    @Test
    fun offlineRefresh_isRetriedOnceAfterDebounce() = runTest(testDispatcher) {
        repository.errorToThrow = DataError.NoConnection()
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        repository.errorToThrow = null
        repository.refreshResult = listOf(rick)
        connectivity.status.value = OFFLINE
        advanceUntilIdle()
        connectivity.status.value = ONLINE
        advanceTimeBy(1_000)
        assertEquals(1, repository.refreshCount)

        advanceUntilIdle()
        assertEquals(2, repository.refreshCount)
        assertEquals(listOf(rick), viewModel.uiState.value.characters)
        assertNull(viewModel.uiState.value.errorMessage)

        // Recovered: the next reconnect makes no request.
        reconnect()
        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun offlineLoadMore_retriesLoadMoreOnly_notRefresh() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        repository.paginationState.value = PaginationState.HasMore
        repository.loadNextPageError = DataError.NoConnection()
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()
        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        repository.loadNextPageError = null
        repository.nextPageResult = listOf(morty)
        repository.paginationStateAfterLoad = PaginationState.EndReached
        reconnect()

        assertEquals(1, repository.refreshCount)
        assertEquals(2, repository.loadNextPageCount)
        assertEquals(listOf(rick, morty), viewModel.uiState.value.characters)
    }

    @Test
    fun successfulRefresh_isNotRepeatedOnReconnect() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        reconnect()

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun hiddenList_makesNoRequest_whileNetworkReturns() = runTest(testDispatcher) {
        repository.errorToThrow = DataError.NoConnection()
        createViewModel()
        advanceUntilIdle()
        repository.errorToThrow = null

        reconnect()

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun tabSwitchWhileOffline_thenBackOnline_recoversOnceOnReturn() = runTest(testDispatcher) {
        repository.errorToThrow = DataError.NoConnection()
        connectivity.status.value = OFFLINE
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()
        assertEquals(1, repository.refreshCount)

        // User switches to another tab; the network returns while this one is hidden.
        viewModel.setVisible(false)
        repository.errorToThrow = null
        repository.refreshResult = listOf(rick)
        connectivity.status.value = ONLINE
        advanceUntilIdle()
        assertEquals(1, repository.refreshCount)

        // Back to this tab: the failed refresh is retried once.
        viewModel.setVisible(true)
        advanceUntilIdle()
        assertEquals(2, repository.refreshCount)
        assertEquals(listOf(rick), viewModel.uiState.value.characters)

        // Recovered: further tab switches make no request.
        viewModel.setVisible(false)
        viewModel.setVisible(true)
        advanceUntilIdle()
        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun returningToScreen_withoutPriorFailure_makesNoRequest() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        repeat(3) {
            viewModel.setVisible(false)
            viewModel.setVisible(true)
            advanceUntilIdle()
        }

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun returningWhileStillFailing_retriesOncePerReturn_noLoop() = runTest(testDispatcher) {
        // The network looks usable, but the request keeps failing (e.g. DNS).
        repository.errorToThrow = DataError.NoConnection()
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        viewModel.setVisible(false)
        viewModel.setVisible(true)
        advanceTimeBy(60_000)

        // One retry for the one return, nothing on its own afterwards.
        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun returnWhileRefreshRunning_doesNotStartASecondRefresh() = runTest(testDispatcher) {
        repository.errorToThrow = DataError.NoConnection()
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        advanceUntilIdle()

        // A manual refresh is in flight when the screen becomes visible again.
        repository.errorToThrow = null
        repository.gate = kotlinx.coroutines.CompletableDeferred()
        viewModel.refreshCharacters()
        advanceUntilIdle()
        viewModel.setVisible(false)
        viewModel.setVisible(true)
        advanceUntilIdle()
        repository.gate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, repository.refreshCount)
    }
}
