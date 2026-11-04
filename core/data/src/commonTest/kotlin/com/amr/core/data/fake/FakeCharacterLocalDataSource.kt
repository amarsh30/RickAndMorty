package com.amr.core.data.fake

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterLocalDataSource : CharacterLocalDataSource {

    private val characters = MutableStateFlow<List<Character>>(
        emptyList()
    )

    override fun observeCharacters(): Flow<List<Character>> {
        return characters
    }

    override suspend fun saveCharacters(
        characters: List<Character>
    ) {
        this.characters.value = characters
    }
}