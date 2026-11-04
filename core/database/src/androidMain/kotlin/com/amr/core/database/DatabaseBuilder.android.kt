package com.amr.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun createDatabaseBuilder(
    context: Context
): RoomDatabase.Builder<CharacterDatabase> {

    val databaseFile = context.getDatabasePath(
        "rick_and_morty.db"
    )

    return Room.databaseBuilder<CharacterDatabase>(
        context = context.applicationContext,
        name = databaseFile.absolutePath
    )
}