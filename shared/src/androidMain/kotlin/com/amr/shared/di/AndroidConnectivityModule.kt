package com.amr.shared.di

import android.content.Context
import com.amr.core.domain.repository.ConnectivityObserver
import com.amr.shared.connectivity.AndroidConnectivityObserver
import org.koin.dsl.module

val androidConnectivityModule = module {
    single<ConnectivityObserver> {
        AndroidConnectivityObserver(context = get<Context>(), applicationScope = get())
    }
}
