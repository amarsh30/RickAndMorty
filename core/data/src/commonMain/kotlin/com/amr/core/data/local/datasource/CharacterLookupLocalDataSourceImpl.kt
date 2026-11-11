package com.amr.core.data.local.datasource

import com.amr.core.data.mapper.toDomain
import com.amr.core.data.mapper.toEntity
import com.amr.core.data.mapper.toFavoriteEntity
import com.amr.core.data.mapper.toSearchEntity
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.dao.FavoriteCharacterDao
import com.amr.core.database.dao.SearchCharacterDao
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * The search cache is bounded to [SEARCH_CACHE_MAX_ENTRIES] rows. Rows are never
 * removed for being old: stale data stays usable offline and is refreshed by the
 * next successful fetch, which is what moves its updatedAt.
 */
class CharacterLookupLocalDataSourceImpl(
    private val characterDao: CharacterDao,
    private val searchCharacterDao: SearchCharacterDao,
    private val favoriteCharacterDao: FavoriteCharacterDao,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : CharacterLookupLocalDataSource {

    override fun observeSearch(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): Flow<List<Character>> {
        return searchCharacterDao.searchCharacters(name, status, species, gender)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeCharacter(id: Int): Flow<Character?> {
        return combine(
            characterDao.observeCharacter(id),
            searchCharacterDao.observeCharacter(id),
            favoriteCharacterDao.observeFavorite(id)
        ) { listRow, searchRow, favoriteRow ->
            listRow?.toDomain() ?: searchRow?.toDomain() ?: favoriteRow?.toDomain()
        }
    }

    override fun observeCharacters(ids: List<Int>): Flow<List<Character>> =
        searchCharacterDao.observeCharactersByIds(ids).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCachedIds(ids: List<Int>): Set<Int> =
        searchCharacterDao.getCachedCharacterIds(ids).toSet()

    /**
     * Each write is idempotent, so a partially applied save is corrected by the
     * next fetch of the same characters.
     */
    override suspend fun saveFetchedCharacters(characters: List<Character>) {
        val outsideList = characters.filter { character ->
            characterDao.updateCharacter(character.toEntity()) == 0
        }
        if (outsideList.isNotEmpty()) {
            val fetchedAt = nowMillis()
            searchCharacterDao.upsertAndEvict(
                characters = outsideList.map { it.toSearchEntity(updatedAt = fetchedAt) },
                maxEntries = SEARCH_CACHE_MAX_ENTRIES
            )
        }

        characters.forEach { character ->
            with(character.toFavoriteEntity()) {
                favoriteCharacterDao.updateFavoriteIfExists(
                    id = id, name = name, status = status, species = species,
                    imageUrl = imageUrl, type = type, gender = gender,
                    originName = originName, originId = originId,
                    locationName = locationName, locationId = locationId,
                    episodeIds = episodeIds
                )
            }
        }
    }

    override fun observeFavorites(): Flow<List<Character>> {
        return favoriteCharacterDao.observeFavorites()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeIsFavorite(id: Int): Flow<Boolean> =
        favoriteCharacterDao.observeIsFavorite(id)

    override suspend fun addFavorite(character: Character) {
        favoriteCharacterDao.upsertFavorite(character.toFavoriteEntity())
    }

    override suspend fun removeFavorite(id: Int) {
        favoriteCharacterDao.deleteFavorite(id)
    }

    internal companion object {
        const val SEARCH_CACHE_MAX_ENTRIES = 1_000
    }
}
