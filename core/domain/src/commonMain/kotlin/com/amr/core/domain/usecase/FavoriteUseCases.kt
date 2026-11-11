package com.amr.core.domain.usecase

import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.FavoriteRepository

class ObserveFavoritesUseCase(
    private val repository: FavoriteRepository
) {
    operator fun invoke() = repository.observeFavorites()
}

class ObserveIsFavoriteUseCase(
    private val repository: FavoriteRepository
) {
    operator fun invoke(id: Int) = repository.observeIsFavorite(id)
}

class SetFavoriteUseCase(
    private val repository: FavoriteRepository
) {
    suspend operator fun invoke(character: Character, favorite: Boolean) {
        if (favorite) {
            repository.addFavorite(character)
        } else {
            repository.removeFavorite(character.id)
        }
    }
}
