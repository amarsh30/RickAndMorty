package com.amr.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.entity.CharacterEntity

@Database(
    entities = [CharacterEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CharacterDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
}