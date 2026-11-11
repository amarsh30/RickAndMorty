package com.amr.feature.character.di

import com.amr.feature.character.detail.CharacterDetailViewModel
import com.amr.feature.character.favorite.FavoritesViewModel
import com.amr.feature.character.presentation.CharacterViewModel
import com.amr.feature.character.search.CharacterSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val characterModule = module {
    viewModelOf(::CharacterViewModel)
    viewModelOf(::CharacterSearchViewModel)
    viewModelOf(::FavoritesViewModel)
    viewModel { (characterId: Int) ->
        CharacterDetailViewModel(
            characterId = characterId,
            observeCharacterDetail = get(),
            refreshCharacterDetail = get(),
            observeIsFavorite = get(),
            setFavorite = get(),
            connectivityRestored = get()
        )
    }
}
