package com.amr.feature.character.di

import com.amr.feature.character.presentation.CharacterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val characterModule = module {
    viewModelOf(::CharacterViewModel)
}