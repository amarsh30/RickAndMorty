package com.amr.core.domain.repository

import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {

    fun observeCharacters(): Flow<List<Character>>

    suspend fun refreshCharacters()
}