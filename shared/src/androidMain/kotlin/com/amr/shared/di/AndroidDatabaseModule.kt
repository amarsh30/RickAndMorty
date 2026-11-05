package com.amr.shared.di

import android.content.Context
import androidx.room.RoomDatabase
import com.amr.core.database.CharacterDatabase
import com.amr.core.database.createDatabaseBuilder
import org.koin.dsl.module

val androidDatabaseModule = module {

    single<RoomDatabase.Builder<CharacterDatabase>> {
        createDatabaseBuilder(
            context = get<Context>()
        )
    }

}