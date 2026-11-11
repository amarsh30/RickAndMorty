package com.amr.core.data.local.datasource

import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

/**
 * Local access for search, detail and favorites. Never changes membership or
 * order of the paginated list cache.
 */
interface CharacterLookupLocalDataSource {

    /** Null filter values match everything. List-cache rows win over search-cache rows. */
    fun observeSearch(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): Flow<List<Character>>

    /** List cache first, then search cache, then the favorite snapshot. */
    fun observeCharacter(id: Int): Flow<Character?>

    /** The cached characters among [ids], one per id, in unspecified order. */
    fun observeCharacters(ids: List<Int>): Flow<List<Character>>

    /** Which of [ids] are cached in any character table. */
    suspend fun getCachedIds(ids: List<Int>): Set<Int>

    /**
     * Stores characters fetched outside list pagination: rows already in the list
     * cache are updated in place, others go to the search cache. Favorite snapshots
     * of these characters are refreshed too.
     */
    suspend fun saveFetchedCharacters(characters: List<Character>)

    fun observeFavorites(): Flow<List<Character>>

    fun observeIsFavorite(id: Int): Flow<Boolean>

    suspend fun addFavorite(character: Character)

    suspend fun removeFavorite(id: Int)
}
