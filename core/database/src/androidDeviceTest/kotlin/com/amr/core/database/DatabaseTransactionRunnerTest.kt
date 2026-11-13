package com.amr.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [RoomDatabaseTransactionRunner] against a real in-memory Room database: DAO calls from
 * several DAOs inside the block share one transaction, so they commit or roll back together.
 */
class DatabaseTransactionRunnerTest {

    private lateinit var database: CharacterDatabase
    private lateinit var runner: DatabaseTransactionRunner

    private val rick = CharacterEntity(1, "Rick Sanchez", "Alive", "Human", null)
    private val morty = SearchCharacterEntity(
        2, "Morty Smith", "Alive", "Human", null, null, null, null, null, null, null, null, updatedAt = 1L
    )
    private val rickFavorite = FavoriteCharacterEntity(
        1, "Rick Sanchez", "Alive", "Human", null, null, null, null, null, null, null, null
    )

    @BeforeTest
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<CharacterDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        runner = RoomDatabaseTransactionRunner(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun writesFromSeveralDaos_areCommittedTogether_andTheResultIsReturned() = runTest {
        val result = runner.runInTransaction {
            database.characterDao().insertCharacters(listOf(rick))
            database.searchCharacterDao().upsertAndEvict(listOf(morty), maxEntries = 10)
            database.favoriteCharacterDao().upsertFavorite(rickFavorite)
            "done"
        }

        assertEquals("done", result)
        assertEquals(listOf(1), database.characterDao().observeCharacters().first().map { it.id })
        assertEquals(1, database.searchCharacterDao().countCharacters())
        assertEquals(listOf(1), database.favoriteCharacterDao().observeFavorites().first().map { it.id })
    }

    @Test
    fun failureInsideTheBlock_rollsBackEveryDao_andIsRethrown() = runTest {
        database.favoriteCharacterDao().upsertFavorite(rickFavorite)

        assertFailsWith<IllegalStateException> {
            runner.runInTransaction {
                database.characterDao().insertCharacters(listOf(rick))
                database.searchCharacterDao().upsertAndEvict(listOf(morty), maxEntries = 10)
                database.favoriteCharacterDao().updateFavoriteIfExists(
                    1, "Changed", "Dead", "Human", null, null, null, null, null, null, null, null
                )
                throw IllegalStateException("fails after the writes")
            }
        }

        // Nothing from the failed block remains, and earlier data is untouched.
        assertTrue(database.characterDao().observeCharacters().first().isEmpty())
        assertEquals(0, database.searchCharacterDao().countCharacters())
        assertEquals("Rick Sanchez", database.favoriteCharacterDao().observeFavorites().first().single().name)
    }
}
