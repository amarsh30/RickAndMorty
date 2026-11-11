package com.amr.shared.di

import com.amr.core.network.HttpClientFactory
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.datasource.CharacterRemoteDataSourceImpl
import com.amr.core.data.remote.datasource.EpisodeRemoteDataSource
import com.amr.core.data.remote.datasource.EpisodeRemoteDataSourceImpl
import com.amr.core.data.remote.datasource.LocationRemoteDataSource
import com.amr.core.data.remote.datasource.LocationRemoteDataSourceImpl
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule = module {

    single<HttpClient> {
        HttpClientFactory.create()
    }

    single<CharacterRemoteDataSource> {
        CharacterRemoteDataSourceImpl(
            client = get()
        )
    }

    single<LocationRemoteDataSource> { LocationRemoteDataSourceImpl(client = get()) }

    single<EpisodeRemoteDataSource> { EpisodeRemoteDataSourceImpl(client = get()) }
}
