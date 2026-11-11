package com.amr.core.data.fake

import com.amr.core.database.dao.FavoriteCharacterDao
import com.amr.core.database.dao.SearchCharacterDao
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * In-memory search DAO. The combined search SQL is verified against real Room in
 * :core:database (SearchAndFavoriteDaoTest), so it is not re-implemented here.
 */
class FakeSearchCharacterDao : SearchCharacterDao {

    val rows = MutableStateFlow<List<SearchCharacterEntity>>(emptyList())

    override suspend fun upsertCharacters(characters: List<SearchCharacterEntity>) {
        val byId = rows.value.associateBy { it.id } + characters.associateBy { it.id }
        rows.value = byId.values.sortedBy { it.id }
    }

    override fun observeCharacter(id: Int): Flow<SearchCharacterEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun countCharacters(): Int = rows.value.size

    /** List-cache rows the fake should treat as cached too (set by tests that need them). */
    val listRows = MutableStateFlow<List<CharacterEntity>>(emptyList())

    override fun observeCharactersByIds(ids: List<Int>): Flow<List<CharacterEntity>> =
        combine(listRows, rows) { list, search ->
            val listIds = list.map { it.id }.toSet()
            list.filter { it.id in ids } + search
                .filter { it.id in ids && it.id !in listIds }
                .map {
                    CharacterEntity(
                        it.id, it.name, it.status, it.species, it.imageUrl, it.type, it.gender,
                        it.originName, it.originId, it.locationName, it.locationId, it.episodeIds
                    )
                }
        }

    override suspend fun getCachedCharacterIds(ids: List<Int>): List<Int> =
        (listRows.value.map { it.id } + rows.value.map { it.id }).filter { it in ids }.distinct()

    /** Last maxEntries passed to [upsertAndEvict]; the eviction SQL itself is tested on Room. */
    var lastMaxEntries: Int? = null

    override suspend fun evictOldest(maxEntries: Int) {
        rows.value = rows.value
            .sortedWith(compareByDescending<SearchCharacterEntity> { it.updatedAt }.thenByDescending { it.id })
            .take(maxEntries)
            .sortedBy { it.id }
    }

    override suspend fun upsertAndEvict(characters: List<SearchCharacterEntity>, maxEntries: Int) {
        lastMaxEntries = maxEntries
        upsertCharacters(characters)
        evictOldest(maxEntries)
    }

    override fun searchCharacters(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): Flow<List<CharacterEntity>> = error("Covered by SearchAndFavoriteDaoTest on Room")
}

class FakeFavoriteCharacterDao : FavoriteCharacterDao {

    val rows = MutableStateFlow<List<FavoriteCharacterEntity>>(emptyList())

    override fun observeFavorites(): Flow<List<FavoriteCharacterEntity>> =
        rows.map { list -> list.sortedWith(compareBy({ it.name }, { it.id })) }

    override fun observeFavorite(id: Int): Flow<FavoriteCharacterEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override fun observeIsFavorite(id: Int): Flow<Boolean> =
        rows.map { list -> list.any { it.id == id } }

    override suspend fun upsertFavorite(favorite: FavoriteCharacterEntity) {
        rows.value = rows.value.filterNot { it.id == favorite.id } + favorite
    }

    override suspend fun updateFavoriteIfExists(
        id: Int,
        name: String,
        status: String,
        species: String,
        imageUrl: String?,
        type: String?,
        gender: String?,
        originName: String?,
        originId: Int?,
        locationName: String?,
        locationId: Int?,
        episodeIds: String?
    ) {
        rows.value = rows.value.map { row ->
            if (row.id != id) row else FavoriteCharacterEntity(
                id, name, status, species, imageUrl, type, gender,
                originName, originId, locationName, locationId, episodeIds
            )
        }
    }

    override suspend fun deleteFavorite(id: Int) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
