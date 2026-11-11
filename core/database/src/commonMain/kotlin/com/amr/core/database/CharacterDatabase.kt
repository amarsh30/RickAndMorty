
package com.amr.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.dao.FavoriteCharacterDao
import com.amr.core.database.dao.SearchCharacterDao
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.CharacterPagingStateEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
import androidx.room.AutoMigration

@Database(
    entities = [
        CharacterEntity::class,
        CharacterPagingStateEntity::class,
        SearchCharacterEntity::class,
        FavoriteCharacterEntity::class
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2
        ),
        // Adds character_paging_state; characters is untouched and no metadata row is created.
        AutoMigration(
            from = 2,
            to = 3
        ),
        // Adds nullable detail columns to characters and the search/favorite tables;
        // existing rows and paging metadata are kept.
        AutoMigration(
            from = 3,
            to = 4
        ),
        // Adds search_characters.updatedAt (default 0) and its index for eviction;
        // no rows are removed.
        AutoMigration(
            from = 4,
            to = 5
        )
    ]
)
@ConstructedBy(CharacterDatabaseConstructor::class)
abstract class CharacterDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao

    abstract fun searchCharacterDao(): SearchCharacterDao

    abstract fun favoriteCharacterDao(): FavoriteCharacterDao
}

@Suppress("KotlinNoActualForExpect")
expect object CharacterDatabaseConstructor :
    RoomDatabaseConstructor<CharacterDatabase> {

    override fun initialize(): CharacterDatabase
}
