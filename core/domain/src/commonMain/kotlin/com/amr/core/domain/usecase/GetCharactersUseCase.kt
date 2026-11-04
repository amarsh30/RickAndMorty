package com.amr.core.domain.usecase

import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow

class GetCharactersUseCase(
    private val repository: CharacterRepository
) {
    operator fun invoke(): Flow<List<Character>> {
        return repository.observeCharacters()
    }
}