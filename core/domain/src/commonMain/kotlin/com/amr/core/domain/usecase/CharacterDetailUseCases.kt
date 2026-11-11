package com.amr.core.domain.usecase

import com.amr.core.domain.repository.CharacterDetailRepository

class ObserveCharacterDetailUseCase(
    private val repository: CharacterDetailRepository
) {
    operator fun invoke(id: Int) = repository.observeCharacter(id)
}

class RefreshCharacterDetailUseCase(
    private val repository: CharacterDetailRepository
) {
    suspend operator fun invoke(id: Int) {
        repository.refreshCharacter(id)
    }
}
