package com.amr.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.amr.core.database.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters ORDER BY id ASC")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Upsert
    suspend fun insertCharacters(
        characters: List<CharacterEntity>
    )
}