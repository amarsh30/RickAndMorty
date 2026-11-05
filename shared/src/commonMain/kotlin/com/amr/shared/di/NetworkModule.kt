package com.amr.shared.di

import com.amr.core.network.HttpClientFactory
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.datasource.CharacterRemoteDataSourceImpl
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
}