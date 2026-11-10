package com.amr.core.domain.fake

import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterRepository : CharacterRepository {

    val characters = MutableStateFlow<List<Character>>(emptyList())

    var refreshResult: List<Character> = emptyList()
    var errorToThrow: Exception? = null

    var refreshCount = 0
        private set

    override fun observeCharacters(): Flow<List<Character>> = characters

    override suspend fun refreshCharacters() {
        refreshCount++
        errorToThrow?.let { throw it }
        characters.value = refreshResult
    }
}
