package com.amr.shared.di

import androidx.room.RoomDatabase
import com.amr.core.database.CharacterDatabase
import com.amr.core.database.createCharacterDatabase
import com.amr.core.database.dao.CharacterDao
import org.koin.dsl.module

val databaseModule = module {

    single<CharacterDatabase> {
        val builder: RoomDatabase.Builder<CharacterDatabase> = get()

        createCharacterDatabase(
            builder = builder
        )
    }

    single<CharacterDao> {
        get<CharacterDatabase>().characterDao()
    }
}