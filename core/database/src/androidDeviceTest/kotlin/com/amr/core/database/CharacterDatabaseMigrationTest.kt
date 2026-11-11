package com.amr.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
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

    @Test
    fun migrate3To4_keepsCharactersAndPagingStateAndAddsEmptyTables() = runTest {
        createVersion3Database()

        val database = Room.databaseBuilder<CharacterDatabase>(context, dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            val dao = database.characterDao()

            // Existing rows survive; the new detail columns are null (not yet loaded).
            assertEquals(
                listOf(
                    CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png"),
                    CharacterEntity(2, "Morty Smith", "Alive", "Human", null)
                ),
                dao.observeCharacters().first()
            )
            val pagingState = dao.getPagingState()
            assertEquals(2, pagingState?.nextPage)
            assertEquals(42, pagingState?.totalPages)

            // New tables exist and are empty.
            assertEquals(
                emptyList(),
                database.favoriteCharacterDao().observeFavorites().first()
            )
            assertNull(database.searchCharacterDao().observeCharacter(1).first())
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate4To5_keepsSearchRowsWithZeroUpdatedAt_andLeavesOtherTablesUntouched() = runTest {
        createVersion4Database()

        val database = Room.databaseBuilder<CharacterDatabase>(context, dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            // Old search row survives; the new column takes its default.
            assertEquals(
                SearchCharacterEntity(
                    7, "Birdperson", "Alive", "Alien", "bird.png", "Bird-Person", "Male",
                    "Bird World", 15, "Planet Squanch", 35, "11,22", updatedAt = 0
                ),
                database.searchCharacterDao().observeCharacter(7).first()
            )
            assertEquals(1, database.searchCharacterDao().countCharacters())

            assertEquals(
                listOf(CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png")),
                database.characterDao().observeCharacters().first()
            )
            val pagingState = database.characterDao().getPagingState()
            assertEquals(2, pagingState?.nextPage)
            assertEquals(42, pagingState?.totalPages)
            assertEquals(
                listOf(
                    FavoriteCharacterEntity(
                        1, "Rick Sanchez", "Alive", "Human", "rick.png",
                        null, null, null, null, null, null, null
                    )
                ),
                database.favoriteCharacterDao().observeFavorites().first()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate5To6_addsEmptyLocationAndEpisodeTables_andKeepsEverythingElse() = runTest {
        createVersion5Database()

        val database = Room.databaseBuilder<CharacterDatabase>(context, dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            assertEquals(
                listOf(CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png")),
                database.characterDao().observeCharacters().first()
            )
            assertEquals(2, database.characterDao().getPagingState()?.nextPage)
            assertEquals(
                1_700_000_000_000L,
                database.searchCharacterDao().observeCharacter(7).first()?.updatedAt
            )
            assertEquals(1, database.favoriteCharacterDao().observeFavorites().first().size)

            // New tables exist and start empty, with no assumed pagination metadata.
            assertEquals(emptyList(), database.locationDao().observeListLocations().first())
            assertEquals(emptyList(), database.locationDao().observeResidentIds(1).first())
            assertNull(database.locationDao().getPagingState())
            assertEquals(emptyList(), database.episodeDao().observeListEpisodes().first())
            assertEquals(emptyList(), database.episodeDao().observeCharacterIds(1).first())
            assertNull(database.episodeDao().getPagingState())
        } finally {
            database.close()
        }
    }

    private fun createVersion5Database() {
        BundledSQLiteDriver().open(dbFile.absolutePath).apply {
            try {
                // Exact statements from schemas/com.amr.core.database.CharacterDatabase/5.json.
                val characterColumns = "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, `species` TEXT NOT NULL, `imageUrl` TEXT, `type` TEXT, " +
                    "`gender` TEXT, `originName` TEXT, `originId` INTEGER, `locationName` TEXT, " +
                    "`locationId` INTEGER, `episodeIds` TEXT"
                execSQL("CREATE TABLE IF NOT EXISTS `characters` ($characterColumns, PRIMARY KEY(`id`))")
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `character_paging_state` (`id` INTEGER NOT NULL, " +
                        "`nextPage` INTEGER, `totalPages` INTEGER, PRIMARY KEY(`id`))"
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `search_characters` ($characterColumns, " +
                        "`updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_search_characters_updatedAt` " +
                        "ON `search_characters` (`updatedAt`)"
                )
                execSQL("CREATE TABLE IF NOT EXISTS `favorite_characters` ($characterColumns, PRIMARY KEY(`id`))")
                execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table " +
                        "(id INTEGER PRIMARY KEY,identity_hash TEXT)"
                )
                execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                        "VALUES(42, '$VERSION_5_IDENTITY_HASH')"
                )
                execSQL(
                    "INSERT INTO characters (id, name, status, species, imageUrl) VALUES " +
                        "(1, 'Rick Sanchez', 'Alive', 'Human', 'rick.png')"
                )
                execSQL(
                    "INSERT INTO character_paging_state (id, nextPage, totalPages) VALUES (0, 2, 42)"
                )
                execSQL(
                    "INSERT INTO search_characters (id, name, status, species, updatedAt) VALUES " +
                        "(7, 'Birdperson', 'Alive', 'Alien', 1700000000000)"
                )
                execSQL(
                    "INSERT INTO favorite_characters (id, name, status, species, imageUrl) VALUES " +
                        "(1, 'Rick Sanchez', 'Alive', 'Human', 'rick.png')"
                )
                execSQL("PRAGMA user_version = 5")
            } finally {
                close()
            }
        }
    }

    private fun createVersion4Database() {
        BundledSQLiteDriver().open(dbFile.absolutePath).apply {
            try {
                // Exact statements from schemas/com.amr.core.database.CharacterDatabase/4.json.
                val characterColumns = "(`id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, `species` TEXT NOT NULL, `imageUrl` TEXT, `type` TEXT, " +
                    "`gender` TEXT, `originName` TEXT, `originId` INTEGER, `locationName` TEXT, " +
                    "`locationId` INTEGER, `episodeIds` TEXT, PRIMARY KEY(`id`))"
                execSQL("CREATE TABLE IF NOT EXISTS `characters` $characterColumns")
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `character_paging_state` (`id` INTEGER NOT NULL, " +
                        "`nextPage` INTEGER, `totalPages` INTEGER, PRIMARY KEY(`id`))"
                )
                execSQL("CREATE TABLE IF NOT EXISTS `search_characters` $characterColumns")
                execSQL("CREATE TABLE IF NOT EXISTS `favorite_characters` $characterColumns")
                execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table " +
                        "(id INTEGER PRIMARY KEY,identity_hash TEXT)"
                )
                execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                        "VALUES(42, '$VERSION_4_IDENTITY_HASH')"
                )
                execSQL(
                    "INSERT INTO characters (id, name, status, species, imageUrl) VALUES " +
                        "(1, 'Rick Sanchez', 'Alive', 'Human', 'rick.png')"
                )
                execSQL(
                    "INSERT INTO character_paging_state (id, nextPage, totalPages) VALUES (0, 2, 42)"
                )
                execSQL(
                    "INSERT INTO search_characters VALUES (7, 'Birdperson', 'Alive', 'Alien', " +
                        "'bird.png', 'Bird-Person', 'Male', 'Bird World', 15, 'Planet Squanch', 35, '11,22')"
                )
                execSQL(
                    "INSERT INTO favorite_characters (id, name, status, species, imageUrl) VALUES " +
                        "(1, 'Rick Sanchez', 'Alive', 'Human', 'rick.png')"
                )
                execSQL("PRAGMA user_version = 4")
            } finally {
                close()
            }
        }
    }

    private fun createVersion3Database() {
        BundledSQLiteDriver().open(dbFile.absolutePath).apply {
            try {
                // Exact statements from schemas/com.amr.core.database.CharacterDatabase/3.json.
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `characters` (`id` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `status` TEXT NOT NULL, `species` TEXT NOT NULL, " +
                        "`imageUrl` TEXT, PRIMARY KEY(`id`))"
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS `character_paging_state` (`id` INTEGER NOT NULL, " +
                        "`nextPage` INTEGER, `totalPages` INTEGER, PRIMARY KEY(`id`))"
                )
                execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table " +
                        "(id INTEGER PRIMARY KEY,identity_hash TEXT)"
                )
                execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                        "VALUES(42, '$VERSION_3_IDENTITY_HASH')"
                )
                execSQL(
                    "INSERT INTO characters (id, name, status, species, imageUrl) VALUES " +
                        "(1, 'Rick Sanchez', 'Alive', 'Human', 'rick.png'), " +
                        "(2, 'Morty Smith', 'Alive', 'Human', NULL)"
                )
                execSQL(
                    "INSERT INTO character_paging_state (id, nextPage, totalPages) VALUES (0, 2, 42)"
                )
                execSQL("PRAGMA user_version = 3")
            } finally {
                close()
            }
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
        const val VERSION_3_IDENTITY_HASH = "a07b30bc02c016e470bd6fe823d662cf"
        const val VERSION_4_IDENTITY_HASH = "4ffcd8b9f9a2c82439811bd9a47ef89f"
        const val VERSION_5_IDENTITY_HASH = "a978173a89776a42765bcf95b14906fa"
    }
}
