package com.amr.core.data.fake

import com.amr.core.database.DatabaseTransactionRunner

/**
 * Runs the block directly and records how it was used. Real rollback and connection
 * sharing are verified against Room in :core:database (DatabaseTransactionRunnerTest).
 */
class FakeTransactionRunner : DatabaseTransactionRunner {
    var transactions = 0
        private set

    /** True while a block is running: lets fakes assert their writes happen inside it. */
    var inTransaction = false
        private set

    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        transactions++
        inTransaction = true
        try {
            return block()
        } finally {
            inTransaction = false
        }
    }
}
