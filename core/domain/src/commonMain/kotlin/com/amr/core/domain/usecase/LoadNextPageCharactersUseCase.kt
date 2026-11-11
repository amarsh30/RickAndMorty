package com.amr.core.domain.usecase

import com.amr.core.domain.repository.CharacterRepository

class LoadNextPageCharactersUseCase(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke() {
        repository.loadNextPage()
    }
}
