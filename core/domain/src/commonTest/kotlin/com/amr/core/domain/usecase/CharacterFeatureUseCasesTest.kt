package com.amr.core.domain.usecase

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.CharacterFilter
import com.amr.core.domain.repository.CharacterDetailRepository
import com.amr.core.domain.repository.CharacterSearchRepository
import com.amr.core.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharacterFeatureUseCasesTest {

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", "rick.png")

    @Test
    fun filter_normalizedTrimsTextAndDetectsEmpty() {
        val filter = CharacterFilter(name = "  rick ", species = " ")

        assertEquals(CharacterFilter(name = "rick", species = ""), filter.normalized())
        assertFalse(filter.isEmpty)
        assertTrue(CharacterFilter(name = " ").isEmpty)
        assertFalse(CharacterFilter(status = CharacterFilter.Status.DEAD).isEmpty)
    }

    @Test
    fun searchUseCases_passNormalizedFilterAndPropagateErrors() = runTest {
        val received = mutableListOf<CharacterFilter>()
        val repository = object : CharacterSearchRepository {
            var error: Exception? = null
            override fun observeSearchResults(filter: CharacterFilter): Flow<List<Character>> {
                received += filter
                return flowOf(listOf(rick))
            }
            override suspend fun searchRemote(filter: CharacterFilter) {
                received += filter
                error?.let { throw it }
            }
        }

        assertEquals(listOf(rick), ObserveCharacterSearchUseCase(repository)(CharacterFilter(name = " rick ")).first())
        SearchCharactersRemoteUseCase(repository)(CharacterFilter(name = "rick "))
        assertEquals(listOf(CharacterFilter(name = "rick"), CharacterFilter(name = "rick")), received)

        repository.error = DataError.NoConnection()
        assertFailsWith<DataError.NoConnection> {
            SearchCharactersRemoteUseCase(repository)(CharacterFilter(name = "rick"))
        }
    }

    @Test
    fun detailUseCases_delegateToRepository() = runTest {
        val refreshed = mutableListOf<Int>()
        val repository = object : CharacterDetailRepository {
            override fun observeCharacter(id: Int): Flow<Character?> = flowOf(rick.takeIf { it.id == id })
            override suspend fun refreshCharacter(id: Int) {
                refreshed += id
            }
        }

        assertEquals(rick, ObserveCharacterDetailUseCase(repository)(1).first())
        RefreshCharacterDetailUseCase(repository)(1)
        assertEquals(listOf(1), refreshed)
    }

    @Test
    fun setFavorite_addsOrRemoves() = runTest {
        val favorites = MutableStateFlow<List<Character>>(emptyList())
        val repository = object : FavoriteRepository {
            override fun observeFavorites(): Flow<List<Character>> = favorites
            override fun observeIsFavorite(id: Int): Flow<Boolean> = flowOf(favorites.value.any { it.id == id })
            override suspend fun addFavorite(character: Character) {
                favorites.value = favorites.value + character
            }
            override suspend fun removeFavorite(id: Int) {
                favorites.value = favorites.value.filterNot { it.id == id }
            }
        }

        SetFavoriteUseCase(repository)(rick, favorite = true)
        assertEquals(listOf(rick), ObserveFavoritesUseCase(repository)().first())
        assertTrue(ObserveIsFavoriteUseCase(repository)(1).first())

        SetFavoriteUseCase(repository)(rick, favorite = false)
        assertTrue(ObserveFavoritesUseCase(repository)().first().isEmpty())
    }
}
