package com.amr.core.database

import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Runs DAO calls from several DAOs as one database transaction: all of them are
 * applied or none are, and observers are invalidated once, after the commit.
 * Exceptions (including cancellation) propagate after the transaction is rolled back.
 */
interface DatabaseTransactionRunner {
    suspend fun <R> runInTransaction(block: suspend () -> R): R
}

/** Room KMP implementation: DAO calls made inside [block] use the transaction's connection. */
class RoomDatabaseTransactionRunner(private val database: RoomDatabase) : DatabaseTransactionRunner {
    override suspend fun <R> runInTransaction(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
