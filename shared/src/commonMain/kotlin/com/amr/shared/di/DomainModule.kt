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
import com.amr.core.domain.usecase.FetchRelatedCharactersUseCase
import com.amr.core.domain.usecase.LoadNextEpisodesPageUseCase
import com.amr.core.domain.usecase.LoadNextLocationsPageUseCase
import com.amr.core.domain.usecase.ObserveEpisodeCharacterIdsUseCase
import com.amr.core.domain.usecase.ObserveEpisodeDetailUseCase
import com.amr.core.domain.usecase.ObserveEpisodePaginationStateUseCase
import com.amr.core.domain.usecase.ObserveEpisodesUseCase
import com.amr.core.domain.usecase.ObserveLocationDetailUseCase
import com.amr.core.domain.usecase.ObserveLocationPaginationStateUseCase
import com.amr.core.domain.usecase.ObserveLocationResidentIdsUseCase
import com.amr.core.domain.usecase.ObserveLocationsUseCase
import com.amr.core.domain.usecase.ObserveRelatedCharactersUseCase
import com.amr.core.domain.usecase.RefreshEpisodeDetailUseCase
import com.amr.core.domain.usecase.RefreshEpisodesUseCase
import com.amr.core.domain.usecase.RefreshLocationDetailUseCase
import com.amr.core.domain.usecase.RefreshLocationsUseCase
import org.koin.core.module.dsl.factoryOf
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

    // Location
    factoryOf(::ObserveLocationsUseCase)
    factoryOf(::ObserveLocationPaginationStateUseCase)
    factoryOf(::RefreshLocationsUseCase)
    factoryOf(::LoadNextLocationsPageUseCase)
    factoryOf(::ObserveLocationDetailUseCase)
    factoryOf(::ObserveLocationResidentIdsUseCase)
    factoryOf(::RefreshLocationDetailUseCase)

    // Episode
    factoryOf(::ObserveEpisodesUseCase)
    factoryOf(::ObserveEpisodePaginationStateUseCase)
    factoryOf(::RefreshEpisodesUseCase)
    factoryOf(::LoadNextEpisodesPageUseCase)
    factoryOf(::ObserveEpisodeDetailUseCase)
    factoryOf(::ObserveEpisodeCharacterIdsUseCase)
    factoryOf(::RefreshEpisodeDetailUseCase)

    // Characters referenced by locations and episodes
    factoryOf(::ObserveRelatedCharactersUseCase)
    factoryOf(::FetchRelatedCharactersUseCase)
}
