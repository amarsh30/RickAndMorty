package com.amr.core.data.concurrency

import com.amr.core.domain.error.DataError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Scope for work that must outlive a single caller (shared refreshes). Lives as
 * long as the DI graph: [close] is called when Koin shuts down, which cancels
 * whatever is still running.
 */
class ApplicationScope(
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    fun close() = scope.cancel()
}

/**
 * Runs one job per key at a time: callers of [run] with the same key while a job
 * is in flight await that job instead of starting another one. Different keys
 * never share work.
 *
 * The job runs in [scope], so a caller that is cancelled only stops waiting; the
 * job (e.g. a transactional Room write) still completes. Every job is bounded by
 * [timeoutMillis], so work nobody awaits any more cannot run forever. The map
 * entry is removed when its job ends, whatever the outcome.
 */
class SingleFlight(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {
    private val lock = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<*>>()

    suspend fun <T> run(key: String, block: suspend () -> T): T {
        val job = lock.withLock {
            @Suppress("UNCHECKED_CAST")
            val running = inFlight[key]?.takeUnless { it.isCompleted } as Deferred<Result<T>>?
            running ?: startJob(key, block)
        }
        // The job never fails itself: its outcome is thrown here, in the caller's frame,
        // so every caller gets the original exception (with its cause) rather than a
        // copy made while crossing the coroutine boundary.
        return job.await().getOrThrow()
    }

    /** Number of jobs currently registered; for tests of the cleanup. */
    suspend fun inFlightCount(): Int = lock.withLock { inFlight.size }

    /** Must be called with [lock] held. */
    private fun <T> startJob(key: String, block: suspend () -> T): Deferred<Result<T>> {
        lateinit var job: Deferred<Result<T>>
        job = scope.async(start = CoroutineStart.LAZY) {
            try {
                withTimeout(timeoutMillis) {
                    // The outcome is captured inside the timeout scope: an exception
                    // leaving it would be replaced by a copy.
                    try {
                        Result.success(block())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Result.failure(e)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                // Awaiting callers would otherwise see a CancellationException and treat
                // it as their own cancellation.
                Result.failure(DataError.Timeout(e))
            } catch (e: CancellationException) {
                // Cancellation raised by the work itself: callers must still see it as such.
                Result.failure(e)
            } finally {
                // Runs even when the job was cancelled, so the entry can never leak.
                withContext(NonCancellable) {
                    lock.withLock { if (inFlight[key] === job) inFlight.remove(key) }
                }
            }
        }
        inFlight[key] = job
        job.start()
        return job
    }

    companion object {
        /** Matches the retry budget of a remote call, plus time for the Room write. */
        const val DEFAULT_TIMEOUT_MILLIS = 35_000L
    }
}
