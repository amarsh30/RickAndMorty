package com.amr.shared.di

import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
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
}
