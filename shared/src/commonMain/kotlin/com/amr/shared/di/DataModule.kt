package com.amr.shared.di

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.local.datasource.CharacterLocalDataSourceImpl
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.repository.CharacterRepositoryImpl
import com.amr.core.domain.repository.CharacterRepository
import org.koin.dsl.module

val dataModule = module {

    single<CharacterLocalDataSource> {
        CharacterLocalDataSourceImpl(
            characterDao = get()
        )
    }

    single<CharacterRepository> {
        CharacterRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get()
        )
    }
}