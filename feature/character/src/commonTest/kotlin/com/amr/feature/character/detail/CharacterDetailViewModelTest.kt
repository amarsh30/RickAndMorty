package com.amr.feature.character.detail

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterDetailRepository
import com.amr.core.domain.repository.FavoriteRepository
import com.amr.core.domain.usecase.ObserveCharacterDetailUseCase
import com.amr.core.domain.usecase.ObserveFavoritesUseCase
import com.amr.core.domain.usecase.ObserveIsFavoriteUseCase
import com.amr.core.domain.usecase.RefreshCharacterDetailUseCase
import com.amr.core.domain.usecase.SetFavoriteUseCase
import com.amr.feature.character.favorite.FavoritesViewModel
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
class CharacterDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", "rick.png")
    private val rickDetail = rick.copy(gender = "Male", originName = "Earth", episodeIds = listOf(1, 2))

    private class FakeDetailRepository : CharacterDetailRepository {
        val cache = MutableStateFlow<Map<Int, Character>>(emptyMap())
        var remote: Character? = null
        var error: Exception? = null

        override fun observeCharacter(id: Int): Flow<Character?> = cache.map { it[id] }

        override suspend fun refreshCharacter(id: Int) {
            error?.let { throw it }
            remote?.let { cache.value = cache.value + (id to it) }
        }
    }

    private class FakeFavoriteRepository : FavoriteRepository {
        val favorites = MutableStateFlow<Map<Int, Character>>(emptyMap())

        override fun observeFavorites(): Flow<List<Character>> = favorites.map { it.values.toList() }
        override fun observeIsFavorite(id: Int): Flow<Boolean> = favorites.map { id in it }
        override suspend fun addFavorite(character: Character) {
            favorites.value = favorites.value + (character.id to character)
        }
        override suspend fun removeFavorite(id: Int) {
            favorites.value = favorites.value - id
        }
    }

    private lateinit var detailRepository: FakeDetailRepository
    private lateinit var favoriteRepository: FakeFavoriteRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        detailRepository = FakeDetailRepository()
        favoriteRepository = FakeFavoriteRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(id: Int = 1) = CharacterDetailViewModel(
        characterId = id,
        observeCharacterDetail = ObserveCharacterDetailUseCase(detailRepository),
        refreshCharacterDetail = RefreshCharacterDetailUseCase(detailRepository),
        observeIsFavorite = ObserveIsFavoriteUseCase(favoriteRepository),
        setFavorite = SetFavoriteUseCase(favoriteRepository)
    )

    @Test
    fun cachedCharacter_isUpdatedWithFetchedDetails() = runTest(testDispatcher) {
        detailRepository.cache.value = mapOf(1 to rick)
        detailRepository.remote = rickDetail

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(rickDetail, state.character)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun offlineWithCache_keepsCachedCharacter_andShowsMessage() = runTest(testDispatcher) {
        detailRepository.cache.value = mapOf(1 to rick)
        detailRepository.error = DataError.NoConnection()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(rick, viewModel.uiState.value.character)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun offlineWithoutCache_showsErrorWithoutCharacter() = runTest(testDispatcher) {
        detailRepository.error = DataError.NoConnection()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.character)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun toggleFavorite_addsThenRemoves_andFavoritesListFollows() = runTest(testDispatcher) {
        detailRepository.cache.value = mapOf(1 to rick)
        val viewModel = createViewModel()
        val favorites = FavoritesViewModel(ObserveFavoritesUseCase(favoriteRepository))
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFavorite)
        assertEquals(listOf(rick), favorites.uiState.value.favorites)

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFavorite)
        assertTrue(favorites.uiState.value.favorites.isEmpty())
    }
}
