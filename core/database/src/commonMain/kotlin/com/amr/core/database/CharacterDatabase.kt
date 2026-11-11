package com.amr.core.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.dao.EpisodeDao
import com.amr.core.database.dao.FavoriteCharacterDao
import com.amr.core.database.dao.LocationDao
import com.amr.core.database.dao.SearchCharacterDao
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.CharacterPagingStateEntity
import com.amr.core.database.entity.EpisodeCharacterEntity
import com.amr.core.database.entity.EpisodeEntity
import com.amr.core.database.entity.EpisodePagingStateEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.LocationEntity
import com.amr.core.database.entity.LocationPagingStateEntity
import com.amr.core.database.entity.LocationResidentEntity
import com.amr.core.database.entity.SearchCharacterEntity

/**
 * Single app database (the name predates locations and episodes).
 * Every migration is additive; exported schemas 1..n must never be edited.
 */
@Database(
    entities = [
        CharacterEntity::class,
        CharacterPagingStateEntity::class,
        SearchCharacterEntity::class,
        FavoriteCharacterEntity::class,
        LocationEntity::class,
        LocationResidentEntity::class,
        LocationPagingStateEntity::class,
        EpisodeEntity::class,
        EpisodeCharacterEntity::class,
        EpisodePagingStateEntity::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        // + character_paging_state
        AutoMigration(from = 2, to = 3),
        // + detail columns on characters, + search_characters, favorite_characters
        AutoMigration(from = 3, to = 4),
        // + search_characters.updatedAt (default 0) and its index
        AutoMigration(from = 4, to = 5),
        // + locations, location_residents, location_paging_state,
        //   episodes, episode_characters, episode_paging_state
        AutoMigration(from = 5, to = 6)
    ]
)
@ConstructedBy(CharacterDatabaseConstructor::class)
abstract class CharacterDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao

    abstract fun searchCharacterDao(): SearchCharacterDao

    abstract fun favoriteCharacterDao(): FavoriteCharacterDao

    abstract fun locationDao(): LocationDao

    abstract fun episodeDao(): EpisodeDao
}

@Suppress("KotlinNoActualForExpect")
expect object CharacterDatabaseConstructor :
    RoomDatabaseConstructor<CharacterDatabase> {

    override fun initialize(): CharacterDatabase
}
