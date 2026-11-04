package com.amr.core.domain.usecase

import com.amr.core.domain.repository.CharacterRepository

class RefreshCharactersUseCase(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke() {
        repository.refreshCharacters()
    }
}