package com.amr.shared.di

import com.amr.core.domain.repository.ConnectivityObserver
import com.amr.shared.connectivity.IosConnectivityObserver
import org.koin.dsl.module

val iosConnectivityModule = module {
    single<ConnectivityObserver> { IosConnectivityObserver(applicationScope = get()) }
}
