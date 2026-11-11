package com.amr.shared.di

import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
import com.amr.core.domain.usecase.ObserveCharacterDetailUseCase
import com.amr.core.domain.usecase.ObserveCharacterSearchUseCase
import com.amr.core.domain.usecase.ObserveFavoritesUseCase
import com.amr.core.domain.usecase.ObserveIsFavoriteUseCase
import com.amr.core.domain.usecase.RefreshCharacterDetailUseCase
import com.amr.core.domain.usecase.SearchCharactersRemoteUseCase
import com.amr.core.domain.usecase.SetFavoriteUseCase
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.ObservePaginationStateUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import org.koin.dsl.module

val domainModule = module {

    factory {
        ObserveCharactersUseCase(
            repository = get()
        )
    }

    factory {
        RefreshCharactersUseCase(
            repository = get()
        )
    }

    factory {
        ObservePaginationStateUseCase(
            repository = get()
        )
    }

    factory {
        LoadNextPageCharactersUseCase(
            repository = get()
        )
    }

    factory { ObserveCharacterSearchUseCase(repository = get()) }
    factory { SearchCharactersRemoteUseCase(repository = get()) }
    factory { ObserveCharacterDetailUseCase(repository = get()) }
    factory { RefreshCharacterDetailUseCase(repository = get()) }
    factory { ObserveFavoritesUseCase(repository = get()) }
    factory { ObserveIsFavoriteUseCase(repository = get()) }
    factory { SetFavoriteUseCase(repository = get()) }
}
