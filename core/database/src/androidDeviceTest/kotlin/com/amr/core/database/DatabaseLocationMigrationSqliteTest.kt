package com.amr.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.entity.FavoriteCharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs the database relocation against real files and the real bundled SQLite: a production
 * Room database whose newest favorite exists only in the WAL (as after the app is killed)
 * is moved, then reopened with SQLite and Room.
 */
class DatabaseLocationMigrationSqliteTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val root = File(context.cacheDir, "db-location-migration-test")
    private val legacyDir = File(root, "Documents")
    private val targetDir = File(root, "Application Support")
    private val name = "rick_and_morty.db"

    @BeforeTest
    fun setUp() {
        root.deleteRecursively()
        legacyDir.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun walOnlyFavorite_survivesMigration_andDestinationPassesIntegrityCheck() = runTest {
        createLegacyDatabaseWithFavoriteInWal()
        assertTrue(File(legacyDir, "$name-wal").length() > 0, "precondition: favorite lives in the WAL")

        val path = resolveDatabasePath(
            legacyDir.path, targetDir.path, name, JavaFileOps, SqliteDatabaseValidator
        )

        val target = File(targetDir, name)
        assertEquals(target.path, path)
        assertFalse(legacyDir.listFiles().orEmpty().any(), "legacy removed after validation")
        assertFalse(File(targetDir, "$name.migrating").exists())

        val connection = BundledSQLiteDriver().open(target.path)
        try {
            connection.prepare("PRAGMA integrity_check").use { assertTrue(it.step()); assertEquals("ok", it.getText(0)) }
            connection.prepare("SELECT name FROM favorite_characters WHERE id = 1").use {
                assertTrue(it.step()); assertEquals("Rick Sanchez", it.getText(0))
            }
        } finally {
            connection.close()
        }

        val database = openRoom(target)
        try {
            assertEquals(listOf(rick), database.favoriteCharacterDao().observeFavorites().first())
        } finally {
            database.close()
        }
    }

    @Test
    fun corruptLegacyDatabase_isKeptInPlace_andNoDestinationIsCreated() {
        val legacy = File(legacyDir, name).apply { writeBytes(ByteArray(8192) { 0x5A }) }

        val path = resolveDatabasePath(
            legacyDir.path, targetDir.path, name, JavaFileOps, SqliteDatabaseValidator
        )

        assertEquals(legacy.path, path)
        assertTrue(legacy.readBytes().all { it == 0x5A.toByte() }, "legacy untouched")
        assertTrue(targetDir.listFiles().orEmpty().isEmpty(), "no destination, no staging leftovers")
    }

    /** Snapshots main + WAL while Room still has the database open, like a killed process. */
    private suspend fun createLegacyDatabaseWithFavoriteInWal() {
        val live = File(root, "live/$name").apply { parentFile!!.mkdirs() }
        val database = openRoom(live)
        try {
            database.favoriteCharacterDao().upsertFavorite(rick)
            live.copyTo(File(legacyDir, name))
            File("${live.path}-wal").copyTo(File(legacyDir, "$name-wal"))
        } finally {
            database.close()
        }
    }

    private fun openRoom(file: File) =
        Room.databaseBuilder<CharacterDatabase>(context, file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val rick = FavoriteCharacterEntity(
        id = 1, name = "Rick Sanchez", status = "Alive", species = "Human", imageUrl = null,
        type = null, gender = "Male", originName = null, originId = null,
        locationName = null, locationId = null, episodeIds = null
    )

    private object JavaFileOps : DatabaseFileOps {
        override fun exists(path: String) = File(path).exists()
        override fun ensureDirectory(path: String) = File(path).run { isDirectory || mkdirs() }
        override fun copy(from: String, to: String) = runCatching {
            File(from).copyTo(File(to)); true
        }.getOrDefault(false)
        override fun move(from: String, to: String) = File(from).renameTo(File(to))
        override fun delete(path: String) = File(path).run { !exists() || delete() }
        override fun sync(path: String) = File(path).let { file ->
            file.isDirectory || runCatching { RandomAccessFile(file, "rw").use { it.fd.sync() }; true }.getOrDefault(false)
        }
    }
}
