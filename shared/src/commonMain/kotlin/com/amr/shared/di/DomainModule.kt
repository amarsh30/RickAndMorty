package com.amr.shared.di

import com.amr.core.domain.usecase.ObserveCharactersUseCase
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
}