package com.amr.shared.di

import org.koin.core.module.Module

val commonModules: List<Module> = listOf(
    networkModule,
    databaseModule,
    dataModule,
    domainModule
)