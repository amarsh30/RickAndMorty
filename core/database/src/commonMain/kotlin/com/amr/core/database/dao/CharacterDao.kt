package com.amr.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.CharacterPagingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters ORDER BY id ASC")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Upsert
    suspend fun insertCharacters(
        characters: List<CharacterEntity>
    )

    @Query("DELETE FROM characters")
    suspend fun deleteAllCharacters()

    @Query("SELECT * FROM characters WHERE id = :id")
    fun observeCharacter(id: Int): Flow<CharacterEntity?>

    /** Updates the row only if it is already in the list cache; returns the number of rows updated. */
    @Update
    suspend fun updateCharacter(character: CharacterEntity): Int

    /** Returns null when no page has been stored yet. */
    @Query("SELECT * FROM character_paging_state WHERE id = ${CharacterPagingStateEntity.SINGLE_ROW_ID}")
    suspend fun getPagingState(): CharacterPagingStateEntity?

    /** Emits null while no page has been stored yet. */
    @Query("SELECT * FROM character_paging_state WHERE id = ${CharacterPagingStateEntity.SINGLE_ROW_ID}")
    fun observePagingState(): Flow<CharacterPagingStateEntity?>

    @Upsert
    suspend fun upsertPagingState(state: CharacterPagingStateEntity)

    /** Atomically replaces the whole cache with page 1 and resets the pagination metadata. */
    @Transaction
    suspend fun replaceFirstPage(
        characters: List<CharacterEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        deleteAllCharacters()
        insertCharacters(characters)
        upsertPagingState(
            CharacterPagingStateEntity(nextPage = nextPage, totalPages = totalPages)
        )
    }

    /** Atomically adds a following page to the cache and advances the pagination metadata. */
    @Transaction
    suspend fun appendPage(
        characters: List<CharacterEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        insertCharacters(characters)
        upsertPagingState(
            CharacterPagingStateEntity(nextPage = nextPage, totalPages = totalPages)
        )
    }
}
