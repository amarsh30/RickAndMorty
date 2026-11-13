package com.amr.shared.di

import androidx.room.RoomDatabase
import com.amr.core.database.CharacterDatabase
import com.amr.core.database.DatabaseTransactionRunner
import com.amr.core.database.RoomDatabaseTransactionRunner
import com.amr.core.database.createCharacterDatabase
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.dao.EpisodeDao
import com.amr.core.database.dao.LocationDao
import com.amr.core.database.dao.FavoriteCharacterDao
import com.amr.core.database.dao.SearchCharacterDao
import org.koin.dsl.module

val databaseModule = module {

    single<CharacterDatabase> {
        val builder: RoomDatabase.Builder<CharacterDatabase> = get()

        createCharacterDatabase(
            builder = builder
        )
    }

    single<DatabaseTransactionRunner> { RoomDatabaseTransactionRunner(get<CharacterDatabase>()) }

    single<CharacterDao> {
        get<CharacterDatabase>().characterDao()
    }

    single<SearchCharacterDao> {
        get<CharacterDatabase>().searchCharacterDao()
    }

    single<FavoriteCharacterDao> {
        get<CharacterDatabase>().favoriteCharacterDao()
    }

    single<LocationDao> { get<CharacterDatabase>().locationDao() }

    single<EpisodeDao> { get<CharacterDatabase>().episodeDao() }
}