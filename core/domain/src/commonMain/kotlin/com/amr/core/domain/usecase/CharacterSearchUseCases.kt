package com.amr.core.domain.usecase

import com.amr.core.domain.model.CharacterFilter
import com.amr.core.domain.repository.CharacterSearchRepository

class ObserveCharacterSearchUseCase(
    private val repository: CharacterSearchRepository
) {
    operator fun invoke(filter: CharacterFilter) =
        repository.observeSearchResults(filter.normalized())
}

class SearchCharactersRemoteUseCase(
    private val repository: CharacterSearchRepository
) {
    suspend operator fun invoke(filter: CharacterFilter) {
        repository.searchRemote(filter.normalized())
    }
}
