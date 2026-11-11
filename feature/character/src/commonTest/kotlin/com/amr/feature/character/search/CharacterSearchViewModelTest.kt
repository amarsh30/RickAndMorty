package com.amr.feature.character.search

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.CharacterFilter
import com.amr.core.domain.repository.CharacterSearchRepository
import com.amr.core.domain.usecase.ObserveCharacterSearchUseCase
import com.amr.core.domain.usecase.SearchCharactersRemoteUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", "rick.png")
    private val morty = Character(2, "Morty Smith", "Alive", "Human", "morty.png")

    /** Name-only search over a mutable cache; remote calls are recorded and scriptable. */
    private class FakeSearchRepository : CharacterSearchRepository {
        val cache = MutableStateFlow<List<Character>>(emptyList())
        val remoteCalls = mutableListOf<CharacterFilter>()
        var remoteResult: List<Character> = emptyList()
        var remoteError: Exception? = null
        var gate: CompletableDeferred<Unit>? = null

        override fun observeSearchResults(filter: CharacterFilter): Flow<List<Character>> =
            cache.map { list -> list.filter { it.name.contains(filter.name, ignoreCase = true) } }

        override suspend fun searchRemote(filter: CharacterFilter) {
            remoteCalls += filter
            gate?.await()
            remoteError?.let { throw it }
            // Deduplicated by id, the same way the real cache upserts.
            cache.value = (cache.value + remoteResult).associateBy { it.id }.values.toList()
        }
    }

    private lateinit var repository: FakeSearchRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSearchRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CharacterSearchViewModel(
        observeCharacterSearch = ObserveCharacterSearchUseCase(repository),
        searchCharactersRemote = SearchCharactersRemoteUseCase(repository),
        connectivityRestored = com.amr.feature.character.fake.connectivityRestored(connectivity)
    )

    private val connectivity = com.amr.feature.character.fake.FakeConnectivityObserver()

    @Test
    fun offlineSearch_isRepeatedWhenNetworkReturns_whileVisible() = runTest(testDispatcher) {
        repository.remoteError = DataError.NoConnection()
        val viewModel = createViewModel()
        viewModel.setVisible(true)
        viewModel.onFilterChange(CharacterFilter(name = "Rick"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOfflineResult)

        connectivity.status.value = com.amr.feature.character.fake.OFFLINE
        advanceUntilIdle()
        repository.remoteError = null
        connectivity.status.value = com.amr.feature.character.fake.ONLINE
        advanceUntilIdle()

        assertEquals(2, repository.remoteCalls.size)
        assertFalse(viewModel.uiState.value.isOfflineResult)
    }

    @Test
    fun typing_isDebounced_onlyLastQueryHitsApi() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "R"))
        advanceTimeBy(100)
        viewModel.onFilterChange(CharacterFilter(name = "Ri"))
        advanceTimeBy(100)
        viewModel.onFilterChange(CharacterFilter(name = "Rick"))
        advanceUntilIdle()

        assertEquals(listOf(CharacterFilter(name = "Rick")), repository.remoteCalls)
    }

    @Test
    fun sameQueryAfterWhitespaceChange_doesNotRequestAgain() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "Rick"))
        advanceUntilIdle()
        viewModel.onFilterChange(CharacterFilter(name = "Rick "))
        advanceUntilIdle()

        assertEquals(1, repository.remoteCalls.size)
    }

    @Test
    fun cachedMatches_shownBeforeRemoteFinishes_thenCompleted() = runTest(testDispatcher) {
        repository.cache.value = listOf(rick)
        repository.remoteResult = listOf(rick, rick.copy(id = 3, name = "Rick Prime"))
        repository.gate = CompletableDeferred()
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "rick"))
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.results)
        assertTrue(viewModel.uiState.value.isSearchingRemote)

        repository.gate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(1, 3), viewModel.uiState.value.results.map { it.id })
        assertFalse(viewModel.uiState.value.isSearchingRemote)
    }

    @Test
    fun newerQuery_cancelsOlderRemoteSearch_andOwnsLoadingFlag() = runTest(testDispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "Rick"))
        advanceUntilIdle()
        viewModel.onFilterChange(CharacterFilter(name = "Morty"))
        advanceUntilIdle()

        assertEquals(2, repository.remoteCalls.size)
        assertTrue(viewModel.uiState.value.isSearchingRemote)
        assertEquals(CharacterFilter(name = "Morty"), viewModel.uiState.value.filter)
    }

    @Test
    fun offline_keepsCachedResults_andFlagsThemIncomplete() = runTest(testDispatcher) {
        repository.cache.value = listOf(rick, morty)
        repository.remoteError = DataError.NoConnection()
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "Morty"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(morty), state.results)
        assertTrue(state.isOfflineResult)
        assertNull(state.errorMessage)
        assertFalse(state.isSearchingRemote)
    }

    @Test
    fun serverError_isShownAsMessage() = runTest(testDispatcher) {
        repository.remoteError = DataError.Server()
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "Rick"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOfflineResult)
        assertTrue(viewModel.uiState.value.errorMessage != null)
    }

    @Test
    fun emptyFilter_doesNotCallApi() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterChange(CharacterFilter(name = "   "))
        advanceUntilIdle()

        assertTrue(repository.remoteCalls.isEmpty())
    }

    @Test
    fun retry_repeatsRemoteSearchForCurrentFilter() = runTest(testDispatcher) {
        repository.remoteError = DataError.NoConnection()
        val viewModel = createViewModel()
        viewModel.onFilterChange(CharacterFilter(name = "Rick"))
        advanceUntilIdle()

        repository.remoteError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repository.remoteCalls.size)
        assertFalse(viewModel.uiState.value.isOfflineResult)
    }
}
