package com.amr.core.data.local.datasource

import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterLocalDataSource {

    fun observeCharacters(): Flow<List<Character>>

    suspend fun saveCharacters(characters: List<Character>)
}