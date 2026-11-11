package com.amr.core.data.concurrency

import com.amr.core.domain.error.DataError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightTest {

    private fun TestScope.singleFlight(timeoutMillis: Long = 10_000) =
        SingleFlight(backgroundScope, timeoutMillis)

    @Test
    fun concurrentCalls_withSameKey_shareOneExecution() = runTest {
        val flight = singleFlight()
        val gate = CompletableDeferred<Unit>()
        var executions = 0

        val first = async { flight.run("location:refresh") { executions++; gate.await(); "page 1" } }
        val second = async { flight.run("location:refresh") { executions++; "never" } }
        runCurrent()
        gate.complete(Unit)

        assertEquals("page 1", first.await())
        assertEquals("page 1", second.await())
        assertEquals(1, executions)
    }

    @Test
    fun differentKeys_areNeverMerged() = runTest {
        val flight = singleFlight()
        val gate = CompletableDeferred<Unit>()

        val list = async { flight.run("episode:refresh") { gate.await(); "list" } }
        val detail1 = async { flight.run("episode:detail:1") { gate.await(); "detail 1" } }
        val detail2 = async { flight.run("episode:detail:2") { gate.await(); "detail 2" } }
        runCurrent()
        assertEquals(3, flight.inFlightCount())
        gate.complete(Unit)

        assertEquals(listOf("list", "detail 1", "detail 2"), listOf(list.await(), detail1.await(), detail2.await()))
    }

    @Test
    fun cancellingOneCaller_doesNotCancelSharedWork() = runTest {
        val flight = singleFlight()
        val gate = CompletableDeferred<Unit>()
        var completedWrite = false

        val cancelled = backgroundScope.async { flight.run("character:refresh") { gate.await(); completedWrite = true; "ok" } }
        val waiting = backgroundScope.async { flight.run("character:refresh") { "unused" } }
        runCurrent()
        cancelled.cancel()
        gate.complete(Unit)

        assertEquals("ok", waiting.await())
        assertTrue(completedWrite)
        assertTrue(cancelled.isCancelled)
    }

    @Test
    fun failure_reachesEveryWaiter_andNextCallStartsFresh() = runTest {
        val flight = singleFlight()
        val gate = CompletableDeferred<Unit>()

        // runCatching: a failing child coroutine would otherwise fail the test itself.
        val first = async { runCatching { flight.run<String>("location:detail:3") { gate.await(); throw DataError.NoConnection() } } }
        val second = async { runCatching { flight.run("location:detail:3") { "unused" } } }
        runCurrent()
        gate.complete(Unit)

        assertIs<DataError.NoConnection>(first.await().exceptionOrNull())
        assertIs<DataError.NoConnection>(second.await().exceptionOrNull())
        advanceUntilIdle()
        assertEquals(0, flight.inFlightCount())

        // A retry after the failure is a new execution, not the cached failure.
        assertEquals("retried", flight.run("location:detail:3") { "retried" })
    }

    @Test
    fun hangingWork_endsAtTimeout_asDataErrorTimeout_andIsCleanedUp() = runTest {
        val flight = singleFlight(timeoutMillis = 5_000)

        val call = async { runCatching { flight.run<String>("episode:refresh") { awaitCancellation() } } }
        advanceTimeBy(5_001)

        // A domain error, not a CancellationException the caller would swallow.
        assertIs<DataError.Timeout>(call.await().exceptionOrNull())
        advanceUntilIdle()
        assertEquals(0, flight.inFlightCount())
    }

    @Test
    fun failure_keepsTheOriginalExceptionAndCause() = runTest {
        val flight = singleFlight()
        val cause = IllegalStateException("network")
        val original = DataError.InvalidData(cause)

        val thrown = assertFailsWith<DataError.InvalidData> {
            flight.run<String>("character:refresh") { throw original }
        }

        kotlin.test.assertSame(original, thrown)
        kotlin.test.assertSame(cause, thrown.cause)
    }

    @Test
    fun completedWork_leavesNoEntryBehind() = runTest {
        val flight = singleFlight()

        repeat(100) { index -> flight.run("character:detail:$index") { index } }
        advanceUntilIdle()

        assertEquals(0, flight.inFlightCount())
    }
}
