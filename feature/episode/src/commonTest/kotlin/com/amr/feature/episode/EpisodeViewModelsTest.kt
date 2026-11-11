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
        override fun observeCharacterIds(id: Int): Flow<List<Int>> = characterIds.map { it[id].orEmpty() }
        override suspend fun refreshEpisode(id: Int) = Unit
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
        LoadNextEpisodesPageUseCase(episodes)
    )

    private fun detailViewModel(id: Int = 1) = EpisodeDetailViewModel(
        id,
        ObserveEpisodeDetailUseCase(episodes),
        ObserveEpisodeCharacterIdsUseCase(episodes),
        RefreshEpisodeDetailUseCase(episodes),
        ObserveRelatedCharactersUseCase(related),
        FetchRelatedCharactersUseCase(related)
    )

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
