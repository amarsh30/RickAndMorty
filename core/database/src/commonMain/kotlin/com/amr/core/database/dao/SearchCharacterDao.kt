package com.amr.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchCharacterDao {

    @Upsert
    suspend fun upsertCharacters(characters: List<SearchCharacterEntity>)

    @Query("SELECT * FROM search_characters WHERE id = :id")
    fun observeCharacter(id: Int): Flow<SearchCharacterEntity?>

    @Query("SELECT COUNT(*) FROM search_characters")
    suspend fun countCharacters(): Int

    /**
     * Keeps the [maxEntries] most recently updated rows and deletes the rest.
     * Ties on updatedAt are broken by id: the lower id is evicted first.
     * Only touches search_characters.
     */
    @Query(
        """
        DELETE FROM search_characters WHERE id IN (
            SELECT id FROM search_characters
            ORDER BY updatedAt DESC, id DESC
            LIMIT -1 OFFSET :maxEntries
        )
        """
    )
    suspend fun evictOldest(maxEntries: Int)

    /** Atomically stores fetched characters and trims the cache to [maxEntries]. */
    @Transaction
    suspend fun upsertAndEvict(characters: List<SearchCharacterEntity>, maxEntries: Int) {
        upsertCharacters(characters)
        evictOldest(maxEntries)
    }

    /**
     * Local search over the list cache and the search cache, without duplicates:
     * when an id exists in both, the list cache row is used.
     * A null filter value matches everything. name and species match partially,
     * status and gender exactly; all comparisons ignore ASCII case.
     */
    @Query(
        """
        SELECT id, name, status, species, imageUrl, type, gender,
               originName, originId, locationName, locationId, episodeIds
        FROM characters
        WHERE (:name IS NULL OR name LIKE '%' || :name || '%')
          AND (:status IS NULL OR status = :status COLLATE NOCASE)
          AND (:species IS NULL OR species LIKE '%' || :species || '%')
          AND (:gender IS NULL OR gender = :gender COLLATE NOCASE)
        UNION ALL
        SELECT id, name, status, species, imageUrl, type, gender,
               originName, originId, locationName, locationId, episodeIds
        FROM search_characters
        WHERE (:name IS NULL OR name LIKE '%' || :name || '%')
          AND (:status IS NULL OR status = :status COLLATE NOCASE)
          AND (:species IS NULL OR species LIKE '%' || :species || '%')
          AND (:gender IS NULL OR gender = :gender COLLATE NOCASE)
          AND id NOT IN (SELECT id FROM characters)
        ORDER BY id ASC
        """
    )
    fun searchCharacters(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): Flow<List<CharacterEntity>>
}
