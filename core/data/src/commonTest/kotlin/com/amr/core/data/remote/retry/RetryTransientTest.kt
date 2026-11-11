package com.amr.core.data.remote.retry

import com.amr.core.domain.model.ApiStatus
import com.amr.core.network.exception.NetworkException
import com.amr.core.network.util.parseRetryAfterMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RetryTransientTest {

    /** Jitter source that always yields the lowest value. */
    private object NoJitter : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private fun TestScope.policy(random: Random = NoJitter) =
        RetryPolicy(random = random, timeSource = testScheduler.timeSource)

    /** A block that fails with the given errors in order, then answers "ok". */
    private class Script(vararg errors: NetworkException) {
        private val queue = ArrayDeque(errors.toList())
        val attemptTimes = mutableListOf<Long>()

        suspend fun TestScope.run(): String {
            attemptTimes += currentTime
            queue.removeFirstOrNull()?.let { throw it }
            return "ok"
        }
    }

    private suspend fun TestScope.retry(script: Script, policy: RetryPolicy = policy()): String =
        retryTransient(policy) { with(script) { run() } }

    // --- what is retried ---

    @Test
    fun timeoutThenSuccess_retriesAfterBaseDelay() = runTest {
        val script = Script(NetworkException.Timeout())

        assertEquals("ok", retry(script))
        assertEquals(listOf(0L, 500L), script.attemptTimes)
    }

    @Test
    fun gatewayErrors_502_503_504_areRetried_withExponentialBackoff() = runTest {
        val script = Script(NetworkException.ServerError(502), NetworkException.ServerError(504))

        assertEquals("ok", retry(script))
        assertEquals(listOf(0L, 500L, 1_500L), script.attemptTimes)
    }

    @Test
    fun stopsAfterThreeAttemptsInTotal() = runTest {
        val script = Script(*Array(5) { NetworkException.ServerError(503) })

        val error = assertFailsWith<NetworkException.ServerError> { retry(script) }

        assertEquals(503, error.statusCode)
        assertEquals(3, script.attemptTimes.size)
    }

    @Test
    fun nonTransientErrors_areNotRetried() = runTest {
        val notRetried = listOf(
            NetworkException.ServerError(500),
            NetworkException.Unauthorized(),
            NetworkException.NotFound(),
            NetworkException.HttpError(403),
            NetworkException.HttpError(400),
            NetworkException.Serialization(),
            // Offline: recovery waits for a connectivity event instead.
            NetworkException.NoInternet()
        )
        notRetried.forEach { error ->
            val script = Script(error)
            assertFailsWith<NetworkException> { retry(script) }
            assertEquals(1, script.attemptTimes.size, "retried ${error::class.simpleName}")
        }
    }

    // --- backoff and jitter ---

    @Test
    fun jitterAddsAtMostHalf_andDelayIsCapped() = runTest {
        val maxJitter = object : Random() {
            override fun nextBits(bitCount: Int): Int = -1 ushr (32 - bitCount)
        }
        val policy = RetryPolicy(random = maxJitter)

        assertTrue(policy.backoffMillis(1) in 500L..750L)
        assertTrue(policy.backoffMillis(2) in 1_000L..1_500L)
        assertEquals(4_000L, policy.backoffMillis(10))
        assertEquals(500L, RetryPolicy(random = NoJitter).backoffMillis(1))
    }

    // --- 429 and Retry-After ---

    @Test
    fun tooManyRequests_waitsForRetryAfter() = runTest {
        val script = Script(NetworkException.TooManyRequests(retryAfterMillis = 2_000))

        assertEquals("ok", retry(script))
        assertEquals(listOf(0L, 2_000L), script.attemptTimes)
    }

    @Test
    fun tooManyRequests_withoutRetryAfter_usesBackoff() = runTest {
        val script = Script(NetworkException.TooManyRequests(retryAfterMillis = null))

        assertEquals("ok", retry(script))
        assertEquals(listOf(0L, 500L), script.attemptTimes)
    }

    @Test
    fun retryAfterBeyondBudget_failsWithoutRetryingEarly() = runTest {
        val script = Script(NetworkException.TooManyRequests(retryAfterMillis = 40_000))

        assertFailsWith<NetworkException.TooManyRequests> { retry(script) }

        assertEquals(listOf(0L), script.attemptTimes)
        assertEquals(0L, currentTime)
    }

    @Test
    fun retryAfterHeader_secondsAndHttpDate_areParsed() {
        assertEquals(120_000L, parseRetryAfterMillis("120"))
        assertEquals(0L, parseRetryAfterMillis("0"))
        // Wed, 21 Oct 2015 07:28:00 GMT = 1445412480000; "now" 30 s earlier.
        assertEquals(30_000L, parseRetryAfterMillis("Wed, 21 Oct 2015 07:28:00 GMT", nowMillis = 1_445_412_450_000))
        // A date already in the past means no wait.
        assertEquals(0L, parseRetryAfterMillis("Wed, 21 Oct 2015 07:28:00 GMT", nowMillis = 1_445_412_490_000))
        assertNull(parseRetryAfterMillis(null))
        assertNull(parseRetryAfterMillis("soon"))
        assertNull(parseRetryAfterMillis("-5"))
    }

    // --- budget and cancellation ---

    @Test
    fun hangingRequest_endsWithTimeoutAtTheOperationBudget() = runTest {
        val error = assertFailsWith<NetworkException.Timeout> {
            retryTransient(policy()) { awaitCancellation() }
        }

        assertIs<NetworkException.Timeout>(error)
        assertEquals(30_000L, currentTime)
    }

    @Test
    fun backoffThatWouldExceedBudget_isNotStarted() = runTest {
        val tight = RetryPolicy(random = NoJitter, operationTimeoutMillis = 400, timeSource = testScheduler.timeSource)
        val script = Script(NetworkException.Timeout())

        assertFailsWith<NetworkException.Timeout> { retry(script, tight) }

        assertEquals(1, script.attemptTimes.size)
    }

    @Test
    fun callerCancellation_propagates_andStopsFurtherAttempts() = runTest {
        val script = Script(*Array(5) { NetworkException.Timeout() })
        var outcome: Throwable? = null

        val job = launch {
            try {
                retry(script)
            } catch (e: Throwable) {
                outcome = e
                throw e
            }
        }
        runCurrent()
        advanceTimeBy(100)
        job.cancel()
        advanceTimeBy(10_000)

        assertIs<CancellationException>(outcome)
        assertEquals(1, script.attemptTimes.size)
    }

    // --- API status recording ---

    @Test
    fun remoteCalls_recordApiStatusFromFinalOutcome() = runTest {
        val tracker = ApiStatusTracker()
        val calls = RemoteCalls(policy(), tracker)

        assertEquals(ApiStatus.Unknown, tracker.apiStatus.value)

        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.ServerError(500) } }
        assertEquals(ApiStatus.Degraded, tracker.apiStatus.value)

        calls.get { "ok" }
        assertEquals(ApiStatus.Reachable, tracker.apiStatus.value)

        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.NoInternet() } }
        assertEquals(ApiStatus.Unknown, tracker.apiStatus.value)

        // The API answered 404: it is reachable.
        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.NotFound() } }
        assertEquals(ApiStatus.Reachable, tracker.apiStatus.value)
    }

    @Test
    fun apiStatus_distinguishesNoNetwork_timeout_serverError_andRecoversOnSuccess() = runTest {
        val tracker = ApiStatusTracker()
        val calls = RemoteCalls(policy(), tracker)

        // No network: says nothing about the API.
        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.NoInternet() } }
        assertEquals(ApiStatus.Unknown, tracker.apiStatus.value)

        // Timeout (after its retries): network or server, unknown -> not "server degraded".
        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.Timeout() } }
        assertEquals(ApiStatus.Unstable, tracker.apiStatus.value)

        // A real server failure.
        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.ServerError(503) } }
        assertEquals(ApiStatus.Degraded, tracker.apiStatus.value)

        // Any success clears both problem states.
        calls.get { "ok" }
        assertEquals(ApiStatus.Reachable, tracker.apiStatus.value)

        assertFailsWith<NetworkException> { calls.get<String> { throw NetworkException.Timeout() } }
        calls.get { "ok" }
        assertEquals(ApiStatus.Reachable, tracker.apiStatus.value)
    }
}
