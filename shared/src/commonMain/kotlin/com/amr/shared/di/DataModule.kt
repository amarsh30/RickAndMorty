package com.amr.shared.di

import com.amr.core.data.concurrency.ApplicationScope
import com.amr.core.data.concurrency.SingleFlight
import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.local.datasource.CharacterLocalDataSourceImpl
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSourceImpl
import com.amr.core.data.remote.retry.ApiStatusTracker
import com.amr.core.data.remote.retry.RemoteCalls
import com.amr.core.data.remote.retry.RetryPolicy
import com.amr.core.data.repository.CharacterDetailRepositoryImpl
import com.amr.core.data.repository.CharacterRepositoryImpl
import com.amr.core.data.repository.CharacterSearchRepositoryImpl
import com.amr.core.data.repository.EpisodeRepositoryImpl
import com.amr.core.data.repository.FavoriteRepositoryImpl
import com.amr.core.data.repository.LocationRepositoryImpl
import com.amr.core.data.repository.RelatedCharacterRepositoryImpl
import com.amr.core.domain.repository.ApiStatusRepository
import com.amr.core.domain.repository.CharacterDetailRepository
import com.amr.core.domain.repository.CharacterRepository
import com.amr.core.domain.repository.CharacterSearchRepository
import com.amr.core.domain.repository.EpisodeRepository
import com.amr.core.domain.repository.FavoriteRepository
import com.amr.core.domain.repository.LocationRepository
import com.amr.core.domain.repository.RelatedCharacterRepository
import org.koin.dsl.module
import org.koin.dsl.onClose

val dataModule = module {

    // --- reliability infrastructure (one instance each, shared by all repositories) ---

    /** Cancelled when Koin stops, so shared work never outlives the DI graph. */
    single { ApplicationScope() } onClose { it?.close() }

    single { SingleFlight(scope = get<ApplicationScope>().scope) }

    single { ApiStatusTracker() }
    single<ApiStatusRepository> { get<ApiStatusTracker>() }

    single { RemoteCalls(retryPolicy = RetryPolicy(), apiStatusTracker = get()) }

    // --- local data sources ---

    single<CharacterLocalDataSource> {
        CharacterLocalDataSourceImpl(
            characterDao = get()
        )
    }

    single<CharacterLookupLocalDataSource> {
        CharacterLookupLocalDataSourceImpl(
            characterDao = get(),
            searchCharacterDao = get(),
            favoriteCharacterDao = get(),
            transactionRunner = get()
        )
    }

    // --- repositories: singletons, each keeps a paging mutex or in-flight state ---

    single<CharacterRepository> {
        CharacterRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get(),
            remoteCalls = get(),
            singleFlight = get()
        )
    }

    single<CharacterSearchRepository> {
        CharacterSearchRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get(),
            remoteCalls = get()
        )
    }

    single<CharacterDetailRepository> {
        CharacterDetailRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get(),
            remoteCalls = get(),
            singleFlight = get()
        )
    }

    single<FavoriteRepository> {
        FavoriteRepositoryImpl(
            localDataSource = get()
        )
    }

    single<LocationRepository> {
        LocationRepositoryImpl(remoteDataSource = get(), locationDao = get(), remoteCalls = get(), singleFlight = get())
    }

    single<EpisodeRepository> {
        EpisodeRepositoryImpl(remoteDataSource = get(), episodeDao = get(), remoteCalls = get(), singleFlight = get())
    }

    single<RelatedCharacterRepository> {
        RelatedCharacterRepositoryImpl(remoteDataSource = get(), localDataSource = get(), remoteCalls = get())
    }
}
