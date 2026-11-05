package com.amr.shared.di

import org.koin.core.module.Module
import com.amr.feature.character.di.characterModule

val commonModules: List<Module> = listOf(
    networkModule,
    databaseModule,
    dataModule,
    domainModule,
    characterModule
)