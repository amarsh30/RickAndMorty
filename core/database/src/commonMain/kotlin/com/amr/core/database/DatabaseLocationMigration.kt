package com.amr.core.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE

/** Minimal file operations needed to relocate the database; each returns false on failure. */
internal interface DatabaseFileOps {
    fun exists(path: String): Boolean
    fun ensureDirectory(path: String): Boolean
    fun copy(from: String, to: String): Boolean
    fun move(from: String, to: String): Boolean
    fun delete(path: String): Boolean

    /** Flushes a file (or directory entry) to stable storage. */
    fun sync(path: String): Boolean
}

/** Decides whether a database file is a complete, readable Room database. */
internal fun interface DatabaseValidator {
    fun isValid(path: String): Boolean
}

/** Every file SQLite may keep next to the main database. */
internal val SqliteFileSuffixes = listOf("", "-wal", "-shm", "-journal")

/**
 * Files that carry data. `-shm` is only an index that SQLite rebuilds from the WAL,
 * so it is never copied.
 */
private val DataFileSuffixes = listOf("", "-wal", "-journal")

private const val STAGING_SUFFIX = ".migrating"

/**
 * Picks the database path, moving an existing database from [legacyDir] to [targetDir].
 * Must run before any connection to either location is opened.
 *
 * The target main file only ever appears through one atomic rename of a copy that SQLite
 * has already opened, checked and flushed to disk. So if the target exists it is the
 * authoritative database and is never replaced by the legacy one. The legacy files are
 * deleted only while the target validates, and that cleanup is retried on later launches.
 *
 * Migration steps:
 * 1. Copy the legacy main file, `-wal` and `-journal` to `<target>.migrating`, keeping the
 *    SQLite sidecar names so SQLite treats them as one database.
 * 2. Validate the staging copy with SQLite. Opening it replays committed WAL frames, and
 *    closing it checkpoints them into the main file, so the result is a single file.
 * 3. Flush it to disk and rename it to the target.
 * 4. Delete the legacy files.
 *
 * If any step before the rename fails, the staging files are removed and the legacy
 * database stays in use, unchanged. A new empty database is never started while a
 * legacy database exists.
 */
internal fun resolveDatabasePath(
    legacyDir: String,
    targetDir: String,
    fileName: String,
    ops: DatabaseFileOps,
    validator: DatabaseValidator
): String {
    val legacy = "$legacyDir/$fileName"
    val target = "$targetDir/$fileName"
    val staging = target + STAGING_SUFFIX

    fun removeStaging() = SqliteFileSuffixes.forEach { ops.delete(staging + it) }

    if (ops.exists(target)) {
        removeStaging()
        val leftovers = SqliteFileSuffixes.filter { ops.exists(legacy + it) }
        // Only delete the old copy while the active database is known to be good.
        if (leftovers.isNotEmpty() && validator.isValid(target)) {
            leftovers.forEach { ops.delete(legacy + it) }
        }
        return target
    }

    if (!ops.ensureDirectory(targetDir)) return legacy

    // The target main file is absent, so any target sidecar is an orphan. Leaving one in
    // place could let SQLite pair it with a fresh database.
    SqliteFileSuffixes.drop(1).forEach { ops.delete(target + it) }
    removeStaging()

    if (!ops.exists(legacy)) return target

    for (suffix in DataFileSuffixes.filter { ops.exists(legacy + it) }) {
        if (!ops.copy(legacy + suffix, staging + suffix)) {
            removeStaging()
            return legacy
        }
    }

    // A valid staging copy must have folded its WAL or journal into the main file on close.
    val selfContained = validator.isValid(staging) &&
        !ops.exists("$staging-wal") && !ops.exists("$staging-journal")
    ops.delete("$staging-shm")
    if (!selfContained || !ops.sync(staging)) {
        removeStaging()
        return legacy
    }

    ops.move(staging, target)
    // Trust the file system over the return value: once the target exists it is the
    // authoritative database, even if the rename reported an error.
    if (!ops.exists(target)) {
        removeStaging()
        return legacy
    }
    ops.sync(targetDir)

    // Best-effort cleanup. Anything left behind is removed on a later launch.
    SqliteFileSuffixes.forEach { ops.delete(legacy + it) }
    return target
}

/**
 * Opens the file with the bundled SQLite driver (never creating it) and accepts it only if
 * it passes `integrity_check`, has a Room schema version, and its favorites table (when
 * present) is readable. Opening it may checkpoint its WAL, which keeps all committed data.
 */
internal object SqliteDatabaseValidator : DatabaseValidator {
    override fun isValid(path: String): Boolean = try {
        val connection = BundledSQLiteDriver().open(path, SQLITE_OPEN_READWRITE)
        try {
            connection.isIntact()
        } finally {
            connection.close()
        }
    } catch (e: Exception) {
        false
    }

    private fun SQLiteConnection.isIntact(): Boolean {
        if (queryText("PRAGMA integrity_check") != "ok") return false
        if ((queryLong("PRAGMA user_version") ?: 0L) < 1L) return false
        if (!hasTable("room_master_table")) return false
        if (hasTable("favorite_characters")) {
            queryLong("SELECT COUNT(*) FROM favorite_characters") ?: return false
        }
        return true
    }

    private fun SQLiteConnection.hasTable(name: String): Boolean =
        queryLong("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$name'") == 1L

    private fun SQLiteConnection.queryText(sql: String): String? {
        val statement = prepare(sql)
        try {
            return if (statement.step()) statement.getText(0) else null
        } finally {
            statement.close()
        }
    }

    private fun SQLiteConnection.queryLong(sql: String): Long? {
        val statement = prepare(sql)
        try {
            return if (statement.step()) statement.getLong(0) else null
        } finally {
            statement.close()
        }
    }
}
