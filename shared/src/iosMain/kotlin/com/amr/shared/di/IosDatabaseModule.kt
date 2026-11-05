package com.amr.shared.di

import androidx.room.RoomDatabase
import com.amr.core.database.CharacterDatabase
import com.amr.core.database.createDatabaseBuilder
import org.koin.dsl.module

val iosDatabaseModule = module {

    single<RoomDatabase.Builder<CharacterDatabase>> {
        createDatabaseBuilder()
    }

}