package com.amr.shared.di

import org.koin.core.module.Module
import com.amr.feature.character.di.characterModule
import com.amr.feature.episode.di.episodeModule
import com.amr.feature.location.di.locationModule

val commonModules: List<Module> = listOf(
    networkModule,
    databaseModule,
    dataModule,
    domainModule,
    characterModule,
    locationModule,
    episodeModule
)
