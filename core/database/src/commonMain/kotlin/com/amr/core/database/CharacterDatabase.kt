
package com.amr.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.entity.CharacterEntity
import androidx.room.AutoMigration

@Database(
    entities = [CharacterEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2
        )
    ]
)
@ConstructedBy(CharacterDatabaseConstructor::class)
abstract class CharacterDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
}

@Suppress("KotlinNoActualForExpect")
expect object CharacterDatabaseConstructor :
    RoomDatabaseConstructor<CharacterDatabase> {

    override fun initialize(): CharacterDatabase
}
