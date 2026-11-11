package com.amr.shared.di

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.local.datasource.CharacterLocalDataSourceImpl
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSourceImpl
import com.amr.core.data.repository.CharacterDetailRepositoryImpl
import com.amr.core.data.repository.CharacterRepositoryImpl
import com.amr.core.data.repository.CharacterSearchRepositoryImpl
import com.amr.core.data.repository.EpisodeRepositoryImpl
import com.amr.core.data.repository.FavoriteRepositoryImpl
import com.amr.core.data.repository.LocationRepositoryImpl
import com.amr.core.data.repository.RelatedCharacterRepositoryImpl
import com.amr.core.domain.repository.CharacterDetailRepository
import com.amr.core.domain.repository.CharacterRepository
import com.amr.core.domain.repository.CharacterSearchRepository
import com.amr.core.domain.repository.EpisodeRepository
import com.amr.core.domain.repository.FavoriteRepository
import com.amr.core.domain.repository.LocationRepository
import com.amr.core.domain.repository.RelatedCharacterRepository
import org.koin.dsl.module

val dataModule = module {

    single<CharacterLocalDataSource> {
        CharacterLocalDataSourceImpl(
            characterDao = get()
        )
    }

    single<CharacterLookupLocalDataSource> {
        CharacterLookupLocalDataSourceImpl(
            characterDao = get(),
            searchCharacterDao = get(),
            favoriteCharacterDao = get()
        )
    }

    single<CharacterRepository> {
        CharacterRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get()
        )
    }

    single<CharacterSearchRepository> {
        CharacterSearchRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get()
        )
    }

    single<CharacterDetailRepository> {
        CharacterDetailRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get()
        )
    }

    single<FavoriteRepository> {
        FavoriteRepositoryImpl(
            localDataSource = get()
        )
    }

    // Singletons: each keeps a paging mutex or in-flight request state.
    single<LocationRepository> { LocationRepositoryImpl(remoteDataSource = get(), locationDao = get()) }

    single<EpisodeRepository> { EpisodeRepositoryImpl(remoteDataSource = get(), episodeDao = get()) }

    single<RelatedCharacterRepository> {
        RelatedCharacterRepositoryImpl(remoteDataSource = get(), localDataSource = get())
    }
}
