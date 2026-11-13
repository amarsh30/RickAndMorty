package com.amr.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * State-machine tests on an in-memory file system. Real SQLite behaviour is covered by
 * DatabaseLocationMigrationSqliteTest (androidDeviceTest).
 */
class DatabaseLocationMigrationTest {

    private val legacyDir = "/app/Documents"
    private val targetDir = "/app/Library/Application Support"
    private val name = "rick_and_morty.db"
    private val legacy = "$legacyDir/$name"
    private val target = "$targetDir/$name"
    private val staging = "$target.migrating"

    /** In-memory file system: path -> contents. Individual operations can be made to fail. */
    private class FakeFs : DatabaseFileOps {
        val files = mutableMapOf<String, String>()
        val dirs = mutableSetOf<String>()
        val failCopyOf = mutableSetOf<String>()
        val failMoveTo = mutableSetOf<String>()
        val failDelete = mutableSetOf<String>()
        var failEnsureDir = false
        var failSync = false
        var moveSucceedsButReportsFailure = false
        val synced = mutableListOf<String>()

        override fun exists(path: String) = path in files
        override fun ensureDirectory(path: String): Boolean {
            if (failEnsureDir) return false
            dirs += path; return true
        }

        override fun copy(from: String, to: String): Boolean {
            if (from !in files || to in files) return false
            if (from in failCopyOf) {
                files[to] = "partial"; return false // interrupted copy leaves a truncated file
            }
            files[to] = files.getValue(from); return true
        }

        override fun move(from: String, to: String): Boolean {
            if (to in failMoveTo || from !in files || to in files) return false
            files[to] = files.remove(from)!!
            return !moveSucceedsButReportsFailure
        }

        override fun delete(path: String): Boolean {
            if (path in failDelete) return false
            files.remove(path); return true
        }

        override fun sync(path: String): Boolean {
            synced += path; return !failSync
        }
    }

    /**
     * Mimics SQLite: a database is valid when its main file is not corrupt or empty, and
     * closing it checkpoints the WAL into the main file and removes the sidecars.
     */
    private class FakeValidator(private val fs: FakeFs) : DatabaseValidator {
        val validated = mutableListOf<String>()
        var leaveWalBehind = false

        override fun isValid(path: String): Boolean {
            validated += path
            val main = fs.files[path] ?: return false
            if (main == "corrupt" || main == "" || main == "partial") return false
            val wal = fs.files["$path-wal"]
            if (wal != null && !leaveWalBehind) {
                fs.files[path] = "$main+$wal"
                fs.files.remove("$path-wal")
            }
            fs.files.remove("$path-shm")
            return true
        }
    }

    private fun legacyWithFavorites() = FakeFs().apply {
        files[legacy] = "favorites"
        files["$legacy-wal"] = "newest favorite"
        files["$legacy-shm"] = "shm"
    }

    private fun FakeFs.resolve(validator: DatabaseValidator = FakeValidator(this)) =
        resolveDatabasePath(legacyDir, targetDir, name, this, validator)

    private fun FakeFs.stagingFiles() = files.keys.filter { it.startsWith(staging) }
    private fun FakeFs.legacyFiles() = files.keys.filter { it.startsWith(legacyDir) }

    @Test
    fun freshInstall_usesTargetDirectory() {
        val fs = FakeFs()
        assertEquals(target, fs.resolve())
        assertTrue(targetDir in fs.dirs)
        assertTrue(fs.files.isEmpty())
    }

    @Test
    fun migration_preservesFavoritesIncludingWal_andRemovesLegacyAfterValidation() {
        val fs = legacyWithFavorites()

        assertEquals(target, fs.resolve())

        assertEquals("favorites+newest favorite", fs.files[target], "WAL frames folded into the database")
        assertEquals(setOf(target), fs.files.keys, "single self-contained file, legacy and staging gone")
        assertTrue(staging in fs.synced && targetDir in fs.synced, "data flushed before and after rename")
    }

    @Test
    fun shmIsNeverCopied() {
        val fs = legacyWithFavorites()
        val copies = mutableListOf<String>()
        val recording = object : DatabaseFileOps by fs {
            override fun copy(from: String, to: String) = fs.copy(from, to).also { copies += from }
        }

        resolveDatabasePath(legacyDir, targetDir, name, recording, FakeValidator(fs))

        assertEquals(listOf(legacy, "$legacy-wal"), copies)
    }

    @Test
    fun failedCopy_removesPartialStagingData_andKeepsLegacyIntact() {
        val fs = legacyWithFavorites().apply { failCopyOf += "$legacy-wal" }
        val before = fs.files.toMap()

        assertEquals(legacy, fs.resolve())

        assertEquals(before, fs.files)
    }

    @Test
    fun invalidStagingCopy_neverReplacesLegacy() {
        val fs = FakeFs().apply { files[legacy] = "corrupt" }

        assertEquals(legacy, fs.resolve())

        assertEquals(mapOf(legacy to "corrupt"), fs.files, "no target, no staging, legacy untouched")
    }

    @Test
    fun walThatSurvivesValidation_abortsInsteadOfPromotingWithoutIt() {
        val fs = legacyWithFavorites()
        val validator = FakeValidator(fs).apply { leaveWalBehind = true }

        assertEquals(legacy, fs.resolve(validator))

        assertFalse(fs.exists(target))
        assertTrue(fs.stagingFiles().isEmpty())
        assertEquals("newest favorite", fs.files["$legacy-wal"])
    }

    @Test
    fun syncFailure_keepsLegacy() {
        val fs = legacyWithFavorites().apply { failSync = true }

        assertEquals(legacy, fs.resolve())

        assertFalse(fs.exists(target))
        assertTrue(fs.stagingFiles().isEmpty())
        assertEquals(3, fs.legacyFiles().size)
    }

    @Test
    fun promotionRenameFailure_keepsLegacy() {
        val fs = legacyWithFavorites().apply { failMoveTo += target }

        assertEquals(legacy, fs.resolve())

        assertFalse(fs.exists(target))
        assertTrue(fs.stagingFiles().isEmpty())
        assertEquals(3, fs.legacyFiles().size)
    }

    @Test
    fun renameReportedAsFailedButApplied_treatsTargetAsAuthoritative() {
        val fs = legacyWithFavorites().apply { moveSucceedsButReportsFailure = true }

        assertEquals(target, fs.resolve(), "otherwise this session would write to a database the next launch ignores")
        assertEquals("favorites+newest favorite", fs.files[target])
    }

    @Test
    fun failedLegacyCleanup_isRetriedOnNextLaunch_withoutRevertingOrRecopying() {
        val fs = legacyWithFavorites().apply { failDelete += "$legacy-wal" }

        assertEquals(target, fs.resolve())
        assertEquals(listOf("$legacy-wal"), fs.legacyFiles())

        // Newer writes happen in the active database before the next launch.
        fs.files[target] = "favorites+newest favorite+added later"
        fs.failDelete.clear()

        assertEquals(target, fs.resolve())
        assertEquals("favorites+newest favorite+added later", fs.files[target])
        assertTrue(fs.legacyFiles().isEmpty())
    }

    @Test
    fun repeatedLaunches_afterMigration_doNotTouchAnything() {
        val fs = legacyWithFavorites()
        fs.resolve()
        val after = fs.files.toMap()

        repeat(3) {
            val validator = FakeValidator(fs)
            assertEquals(target, fs.resolve(validator))
            assertTrue(validator.validated.isEmpty(), "no validation cost once cleanup is done")
        }
        assertEquals(after, fs.files)
    }

    @Test
    fun interruptedMigration_staleStagingIsDiscarded_andMigrationRestartsFromLegacy() {
        val fs = legacyWithFavorites().apply {
            files[staging] = "partial"
            files["$staging-wal"] = "stale"
            files["$target-wal"] = "orphan"
        }

        assertEquals(target, fs.resolve())

        assertEquals("favorites+newest favorite", fs.files[target])
        assertEquals(setOf(target), fs.files.keys)
    }

    @Test
    fun freshInstall_removesOrphanTargetSidecars() {
        val fs = FakeFs().apply { files["$target-wal"] = "orphan"; files["$staging"] = "partial" }

        assertEquals(target, fs.resolve())

        assertTrue(fs.files.isEmpty())
    }

    @Test
    fun existingValidTarget_isUsed_andLegacyLeftoversRemoved() {
        val fs = FakeFs().apply { files[target] = "current"; files[legacy] = "old" }

        assertEquals(target, fs.resolve())

        assertEquals(mapOf(target to "current"), fs.files)
    }

    @Test
    fun existingCorruptTarget_isNeverReplacedByLegacy_andLegacyIsKept() {
        val fs = FakeFs().apply { files[target] = "corrupt"; files[legacy] = "old"; files["$legacy-wal"] = "w" }

        assertEquals(target, fs.resolve())

        assertEquals("corrupt", fs.files[target])
        assertEquals("old", fs.files[legacy], "the only possibly-good copy is not deleted")
        assertEquals("w", fs.files["$legacy-wal"])
    }

    @Test
    fun existingEmptyTarget_isNeverReplacedByLegacy_andLegacyIsKept() {
        val fs = FakeFs().apply { files[target] = ""; files[legacy] = "old" }

        assertEquals(target, fs.resolve())

        assertEquals("old", fs.files[legacy])
    }

    @Test
    fun migratedTarget_staysAuthoritative_evenWhenItsDirectoryCannotBeCreated() {
        val fs = FakeFs().apply { files[target] = "current"; files[legacy] = "old"; failEnsureDir = true }

        assertEquals(target, fs.resolve())
        assertEquals("current", fs.files[target])
    }

    /**
     * Contract relied on by the iOS builder: with no usable target and no legacy database the
     * returned legacy path does not exist, so the builder refuses it instead of letting Room
     * create an empty database there. Nothing is created or deleted.
     */
    @Test
    fun targetDirectoryUnavailable_withoutLegacy_returnsAbsentLegacyPath_andTouchesNothing() {
        val fs = FakeFs().apply { failEnsureDir = true }

        val path = fs.resolve()

        assertEquals(legacy, path)
        assertFalse(fs.exists(path), "caller must refuse a path with no database")
        assertTrue(fs.files.isEmpty())
    }

    @Test
    fun targetDirectoryUnavailable_keepsUsingLegacy() {
        val fs = legacyWithFavorites().apply { failEnsureDir = true }
        val before = fs.files.toMap()

        assertEquals(legacy, fs.resolve())
        assertEquals(before, fs.files)
    }
}
