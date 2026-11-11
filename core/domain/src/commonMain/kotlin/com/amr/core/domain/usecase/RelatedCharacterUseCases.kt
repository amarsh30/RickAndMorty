package com.amr.core.domain.usecase

import com.amr.core.domain.repository.RelatedCharacterRepository

class ObserveRelatedCharactersUseCase(private val repository: RelatedCharacterRepository) {
    operator fun invoke(ids: List<Int>) = repository.observeCharacters(ids)
}

class FetchRelatedCharactersUseCase(private val repository: RelatedCharacterRepository) {
    suspend operator fun invoke(ids: List<Int>) = repository.fetchMissing(ids)
}
