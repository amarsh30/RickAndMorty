package com.amr.feature.episode

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.Episode
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.EpisodeRepository
import com.amr.core.domain.repository.RelatedCharacterRepository
import com.amr.core.domain.usecase.FetchRelatedCharactersUseCase
import com.amr.core.domain.usecase.LoadNextEpisodesPageUseCase
import com.amr.core.domain.usecase.ObserveEpisodeCharacterIdsUseCase
import com.amr.core.domain.usecase.ObserveEpisodeDetailUseCase
import com.amr.core.domain.usecase.ObserveEpisodePaginationStateUseCase
import com.amr.core.domain.usecase.ObserveEpisodesUseCase
import com.amr.core.domain.usecase.ObserveConnectivityRestoredUseCase
import com.amr.core.domain.model.NetworkStatus
import com.amr.core.domain.repository.ConnectivityObserver
import com.amr.core.domain.usecase.ObserveRelatedCharactersUseCase
import com.amr.core.domain.usecase.RefreshEpisodeDetailUseCase
import com.amr.core.domain.usecase.RefreshEpisodesUseCase
import com.amr.feature.episode.detail.EpisodeDetailViewModel
import com.amr.feature.episode.detail.EpisodeDetailViewModel.Companion.CHARACTER_PAGE_SIZE
import com.amr.feature.episode.presentation.EpisodeListViewModel
import com.amr.feature.episode.presentation.STALE_DATA_MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import androidx.lifecycle.ViewModelStore
import com.amr.feature.episode.presentation.DATABASE_ERROR_MESSAGE
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
class EpisodeViewModelsTest {

    private class FakeEpisodeRepository : EpisodeRepository {
        val list = MutableStateFlow<List<Episode>>(emptyList())
        val pagination = MutableStateFlow<PaginationState>(PaginationState.Uninitialized)
        val details = MutableStateFlow<Map<Int, Episode>>(emptyMap())
        val characterIds = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())
        var refreshResult: List<Episode>? = null
        var refreshError: Exception? = null
        var nextPage: List<Episode> = emptyList()
        var loadMoreCalls = 0

        override fun observeEpisodes(): Flow<List<Episode>> = list
        override fun observePaginationState(): Flow<PaginationState> = pagination
        override suspend fun refreshEpisodes() {
            refreshError?.let { throw it }
            refreshResult?.let { list.value = it }
            pagination.value = PaginationState.HasMore
        }
        override suspend fun loadNextPage() {
            loadMoreCalls++
            list.value = list.value + nextPage
            pagination.value = PaginationState.EndReached
        }
        override fun observeEpisode(id: Int): Flow<Episode?> = details.map { it[id] }
        /** Times the character-id query was subscribed to (each one is a Room query in the app). */
        var characterIdSubscriptions = 0
        var characterIdsError: Exception? = null
        override fun observeCharacterIds(id: Int): Flow<List<Int>> = flow {
            characterIdSubscriptions++
            characterIdsError?.let { throw it }
            emitAll(characterIds.map { it[id].orEmpty() })
        }
        var detailRefreshCalls = 0
        override suspend fun refreshEpisode(id: Int) {
            detailRefreshCalls++
            refreshError?.let { throw it }
        }
    }

    private class FakeRelatedRepository : RelatedCharacterRepository {
        val cached = MutableStateFlow<Map<Int, Character>>(emptyMap())
        val fetchCalls = mutableListOf<List<Int>>()
        var fetchError: DataError? = null

        override fun observeCharacters(ids: List<Int>): Flow<List<Character>> =
            cached.map { all -> ids.mapNotNull { all[it] } }

        override suspend fun fetchMissing(ids: List<Int>) {
            fetchCalls += ids
            fetchError?.let { throw it }
            cached.value = cached.value + ids.associateWith { Character(it, "Character $it", "Alive", "Human", null) }
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var episodes: FakeEpisodeRepository
    private lateinit var related: FakeRelatedRepository

    private val pilot = Episode(1, "Pilot", "December 2, 2013", "S01E01")
    private val lawnmower = Episode(2, "Lawnmower Dog", "December 9, 2013", "S01E02")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        episodes = FakeEpisodeRepository()
        related = FakeRelatedRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun listViewModel() = EpisodeListViewModel(
        ObserveEpisodesUseCase(episodes),
        ObserveEpisodePaginationStateUseCase(episodes),
        RefreshEpisodesUseCase(episodes),
        LoadNextEpisodesPageUseCase(episodes),
        ObserveConnectivityRestoredUseCase(connectivity)
    )

    private class FakeConnectivity : ConnectivityObserver {
        val status = MutableStateFlow(NetworkStatus(isConnected = true, isInternetValidated = true))
        override val networkStatus = status
    }

    private val connectivity = FakeConnectivity()

    private fun detailViewModel(id: Int = 1) = EpisodeDetailViewModel(
        id,
        ObserveEpisodeDetailUseCase(episodes),
        ObserveEpisodeCharacterIdsUseCase(episodes),
        RefreshEpisodeDetailUseCase(episodes),
        ObserveRelatedCharactersUseCase(related),
        FetchRelatedCharactersUseCase(related),
        ObserveConnectivityRestoredUseCase(connectivity)
    )

    @Test
    fun detail_offlineRefresh_isRetriedOnReconnect_whileVisible_only() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.refreshError = DataError.NoConnection()
        val visible = detailViewModel()
        visible.setVisible(true)
        val hidden = detailViewModel(id = 2)
        advanceUntilIdle()
        assertEquals(2, episodes.detailRefreshCalls)

        episodes.refreshError = null
        connectivity.status.value = NetworkStatus.Disconnected
        advanceUntilIdle()
        connectivity.status.value = NetworkStatus(isConnected = true, isInternetValidated = true)
        advanceUntilIdle()

        // Only the visible detail refreshed again.
        assertEquals(3, episodes.detailRefreshCalls)
        assertNull(visible.uiState.value.errorMessage)
        assertNotNull(hidden.uiState.value.errorMessage)
    }

    @Test
    fun list_loadsAndPaginatesOnce() = runTest(dispatcher) {
        episodes.refreshResult = listOf(pilot)
        episodes.nextPage = listOf(lawnmower)
        val viewModel = listViewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(1, episodes.loadMoreCalls)
        assertEquals(listOf(pilot, lawnmower), viewModel.uiState.value.episodes)
        assertTrue(viewModel.uiState.value.isEndReached)
    }

    @Test
    fun list_offline_showsReasonWithoutCache_andStaleWarningWithCache() = runTest(dispatcher) {
        episodes.refreshError = DataError.NoConnection()
        val empty = listViewModel()
        advanceUntilIdle()
        assertEquals("Gagal memuat episode. Periksa koneksi internet.", empty.uiState.value.errorMessage)

        episodes.list.value = listOf(pilot)
        val cached = listViewModel()
        advanceUntilIdle()
        assertEquals(STALE_DATA_MESSAGE, cached.uiState.value.errorMessage)
        assertEquals(listOf(pilot), cached.uiState.value.episodes)
    }

    @Test
    fun detail_fetchesOnlyTheFirstCharacterWindow_inApiOrder() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.characterIds.value = mapOf(1 to (50 downTo 1).toList())

        val viewModel = detailViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(pilot, state.episode)
        assertEquals(50, state.characterCount)
        assertEquals((50 downTo 31).toList(), related.fetchCalls.single())
        assertEquals((50 downTo 31).toList(), state.characters.map { it.id })
        assertEquals(CHARACTER_PAGE_SIZE, state.characters.size)
        assertTrue(state.canShowMoreCharacters)
    }

    // --- One shared collection of the character-id query (6C.3A) ---

    @Test
    fun detail_characterIdQuery_isCollectedOnce_acrossObserverFetcherRetryAndWindowGrowth() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.characterIds.value = mapOf(1 to (1..50).toList())
        related.fetchError = DataError.Timeout()
        val viewModel = detailViewModel()
        advanceUntilIdle()

        // Observer and fetcher share one subscription (was two, plus one per retry).
        assertEquals(1, episodes.characterIdSubscriptions)
        assertEquals(1, episodes.characterIds.subscriptionCount.value)
        assertNotNull(viewModel.uiState.value.charactersErrorMessage)

        related.fetchError = null
        viewModel.loadMoreCharacters() // retry the failed window
        advanceUntilIdle()
        viewModel.loadMoreCharacters() // grow the window
        advanceUntilIdle()

        assertEquals(1, episodes.characterIdSubscriptions)
        assertEquals(listOf((1..20).toList(), (1..20).toList(), (1..40).toList()), related.fetchCalls)
        assertEquals(40, viewModel.uiState.value.characters.size)
        assertEquals(50, viewModel.uiState.value.characterCount)
        assertTrue(viewModel.uiState.value.canShowMoreCharacters)
    }

    @Test
    fun detail_idUpdates_stillReachStateAndFetch() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.characterIds.value = mapOf(1 to listOf(1, 2))
        val viewModel = detailViewModel()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.characterCount)

        episodes.characterIds.value = mapOf(1 to listOf(1, 2, 3))
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.characterCount)
        assertEquals(listOf(1, 2, 3), viewModel.uiState.value.characters.map { it.id })
        assertEquals(listOf(1, 2, 3), related.fetchCalls.last())
        assertEquals(1, episodes.characterIdSubscriptions)
    }

    @Test
    fun detail_noCharacters_isEmptyWithoutFetching() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        val viewModel = detailViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.characterCount)
        assertTrue(state.characters.isEmpty())
        assertFalse(state.canShowMoreCharacters)
        assertFalse(state.isLoadingCharacters)
        assertTrue(related.fetchCalls.isEmpty())
    }

    @Test
    fun detail_characterIdQueryFailure_showsDatabaseError_andKeepsTheEpisode() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.characterIdsError = IllegalStateException("db")
        val viewModel = detailViewModel()
        advanceUntilIdle()

        assertEquals(DATABASE_ERROR_MESSAGE, viewModel.uiState.value.charactersErrorMessage)
        assertEquals(pilot, viewModel.uiState.value.episode)
        assertTrue(related.fetchCalls.isEmpty())
    }

    @Test
    fun detail_clearingTheViewModel_cancelsTheSharedCollection() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.characterIds.value = mapOf(1 to listOf(1))
        val store = ViewModelStore()
        store.put("detail", detailViewModel())
        advanceUntilIdle()
        assertEquals(1, episodes.characterIds.subscriptionCount.value)

        store.clear()
        advanceUntilIdle()

        assertEquals(0, episodes.characterIds.subscriptionCount.value)
    }

    @Test
    fun detail_characterFetchFailure_isRetriable() = runTest(dispatcher) {
        episodes.details.value = mapOf(1 to pilot)
        episodes.characterIds.value = mapOf(1 to listOf(1, 2))
        related.fetchError = DataError.Timeout()
        val viewModel = detailViewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.charactersErrorMessage)

        related.fetchError = null
        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.charactersErrorMessage)
        assertEquals(2, viewModel.uiState.value.characters.size)
        assertFalse(viewModel.uiState.value.canShowMoreCharacters)
    }
}
