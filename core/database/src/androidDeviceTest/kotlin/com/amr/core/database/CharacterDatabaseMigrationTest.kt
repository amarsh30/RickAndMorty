package com.amr.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Upgrades a real version-2 database file to the current version through Room's
 * AutoMigration, without room-testing: the v2 file is written with the exact
 * SQL from schemas/.../2.json, then opened by the production Room configuration.
 */
class CharacterDatabaseMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = context.getDatabasePath("migration-test.db")
        deleteDatabaseFiles()
        dbFile.parentFile?.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        deleteDatabaseFiles()
    }

    @Test
    fun migrate2To3_keepsCharactersAndAddsEmptyPagingStateTable() = runTest {
        createVersion2Database()

        val database = Room.databaseBuilder<CharacterDatabase>(context, dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            val dao = database.characterDao()

            assertEquals(
                listOf(
                    CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png"),
                    CharacterEntity(2, "Morty Smith", "Alive", "Human", null)
                ),
                dao.observeCharacters().first()
            )
            // The new table exists (the query succeeds) and holds no assumed metadata.
            assertNull(dao.getPagingState())
        } finally {
            database.close()
        }
    }

    private fun createVersion2Database() {
        BundledSQLiteDriver().open(dbFile.absolutePath).apply {
            try {
                // Exact statements from schemas/com.amr.core.database.CharacterDatabase/2.json.
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `characters` (`id` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `status` TEXT NOT NULL, `species` TEXT NOT NULL, " +
                        "`imageUrl` TEXT, PRIMARY KEY(`id`))"
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table " +
                        "(id INTEGER PRIMARY KEY,identity_hash TEXT)"
                )
                execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                        "VALUES(42, '$VERSION_2_IDENTITY_HASH')"
                )
                execSQL(
                    "INSERT INTO characters (id, name, status, species, imageUrl) VALUES " +
                        "(1, 'Rick Sanchez', 'Alive', 'Human', 'rick.png'), " +
                        "(2, 'Morty Smith', 'Alive', 'Human', NULL)"
                )
                execSQL("PRAGMA user_version = 2")
            } finally {
                close()
            }
        }
    }

    private fun deleteDatabaseFiles() {
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).delete()
        }
    }

    private companion object {
        const val VERSION_2_IDENTITY_HASH = "58c5f7f98fe24528bd012bb8192ae9de"
    }
}
