package com.amr.core.domain.usecase

import com.amr.core.domain.repository.CharacterRepository

class ObservePaginationStateUseCase(
    private val repository: CharacterRepository
) {
    operator fun invoke() = repository.observePaginationState()
}
