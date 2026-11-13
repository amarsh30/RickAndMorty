package com.amr.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSUserDomainMask
import platform.posix.F_FULLFSYNC
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.fcntl
import platform.posix.fsync
import platform.posix.open

private const val DATABASE_FILE_NAME = "rick_and_morty.db"

/**
 * The database lives in Application Support (app-private, not shown in the Files app).
 * Earlier builds kept it in Documents; it is moved over once, before Room opens it,
 * and kept in Documents if the move cannot complete safely.
 */
@OptIn(ExperimentalForeignApi::class)
fun createDatabaseBuilder(): RoomDatabase.Builder<CharacterDatabase> {
    val legacyDir = directoryPath(NSDocumentDirectory, create = false)
    val targetDir = directoryPath(NSApplicationSupportDirectory, create = true)

    val databasePath = when {
        targetDir == null -> "${legacyDir ?: error("Cannot find a database directory")}/$DATABASE_FILE_NAME"
        legacyDir == null -> "$targetDir/$DATABASE_FILE_NAME"
        else -> resolveDatabasePath(
            legacyDir = legacyDir,
            targetDir = targetDir,
            fileName = DATABASE_FILE_NAME,
            ops = NsFileManagerOps,
            validator = SqliteDatabaseValidator
        )
    }

    return Room.databaseBuilder<CharacterDatabase>(
        name = databasePath
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun directoryPath(directory: NSSearchPathDirectory, create: Boolean): String? =
    NSFileManager.defaultManager.URLForDirectory(
        directory = directory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = create,
        error = null
    )?.path

@OptIn(ExperimentalForeignApi::class)
private object NsFileManagerOps : DatabaseFileOps {
    private val fm get() = NSFileManager.defaultManager

    override fun exists(path: String) = fm.fileExistsAtPath(path)

    override fun ensureDirectory(path: String) =
        fm.fileExistsAtPath(path) ||
            fm.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)

    override fun copy(from: String, to: String) = fm.copyItemAtPath(from, toPath = to, error = null)

    override fun move(from: String, to: String) = fm.moveItemAtPath(from, toPath = to, error = null)

    override fun delete(path: String) = !fm.fileExistsAtPath(path) || fm.removeItemAtPath(path, error = null)

    // fsync alone does not flush the drive cache on Apple platforms; F_FULLFSYNC does.
    override fun sync(path: String): Boolean {
        val fd = open(path, O_RDONLY)
        if (fd < 0) return false
        val synced = fcntl(fd, F_FULLFSYNC) == 0 || fsync(fd) == 0
        close(fd)
        return synced
    }
}
