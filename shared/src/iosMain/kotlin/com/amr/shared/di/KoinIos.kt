package com.amr.shared.di

import org.koin.core.context.startKoin

fun initKoinIos() {
    startKoin {
        modules(
            commonModules + iosDatabaseModule
        )
    }
}