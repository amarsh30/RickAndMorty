package com.amr.core.data.remote.retry

import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.TimeSource

/**
 * Bounded retry for idempotent GET requests only.
 *
 * Retried: timeouts, 502/503/504, and 429 (waiting at least `Retry-After`).
 * Never retried: offline (recovery waits for a connectivity event instead),
 * 401/403/404, 500, other 4xx, and unreadable responses.
 */
class RetryPolicy(
    /** Attempts in total, the first one included. */
    val maxAttempts: Int = 3,
    val baseDelayMillis: Long = 500,
    val maxDelayMillis: Long = 4_000,
    /** Budget for all attempts and waits together. */
    val operationTimeoutMillis: Long = 30_000,
    /** Jitter source; tests pass a seeded or fixed [Random]. */
    val random: Random = Random.Default,
    /** Elapsed-time source for the budget; tests pass the virtual-time source. */
    val timeSource: TimeSource = TimeSource.Monotonic
) {
    fun isRetryable(error: NetworkException): Boolean = when (error) {
        is NetworkException.Timeout -> true
        is NetworkException.ServerError -> error.statusCode in TRANSIENT_SERVER_CODES
        is NetworkException.TooManyRequests -> true
        else -> false
    }

    /** Wait before retry number [retry] (1-based): 500 ms, 1 s, ... plus up to +50 % jitter, capped. */
    fun backoffMillis(retry: Int): Long {
        val exponential = baseDelayMillis shl (retry - 1)
        val jitter = random.nextLong(0, exponential / 2 + 1)
        return minOf(exponential + jitter, maxDelayMillis)
    }

    private companion object {
        val TRANSIENT_SERVER_CODES = setOf(502, 503, 504)
    }
}

/**
 * Runs [block], retrying transient [NetworkException]s according to [policy].
 * Cancellation of the caller always propagates. When the whole budget runs out,
 * a [NetworkException.Timeout] is thrown. A final failure is rethrown as the
 * very instance [block] threw.
 */
suspend fun <T> retryTransient(
    policy: RetryPolicy = RetryPolicy(),
    block: suspend () -> T
): T {
    val start = policy.timeSource.markNow()

    val outcome: Outcome<T>? = withTimeoutOrNull(policy.operationTimeoutMillis) {
        var attempt = 1
        var result: Outcome<T>? = null
        while (result == null) {
            try {
                result = Outcome.Success(block())
            } catch (error: NetworkException) {
                val retryAfter = (error as? NetworkException.TooManyRequests)?.retryAfterMillis
                val wait = retryAfter ?: policy.backoffMillis(attempt)
                val remaining = policy.operationTimeoutMillis - start.elapsedNow().inWholeMilliseconds

                // Never retry earlier than the server asked, nor past the budget.
                if (attempt >= policy.maxAttempts || !policy.isRetryable(error) || wait >= remaining) {
                    result = Outcome.Failure(error)
                } else {
                    delay(wait)
                    attempt++
                }
            }
        }
        result
    }

    // Thrown here, outside the timeout scope, so the coroutine machinery does not
    // replace the exception with a copy on its way out.
    return when (outcome) {
        null -> throw NetworkException.Timeout()
        is Outcome.Success -> outcome.value
        is Outcome.Failure -> throw outcome.error
    }
}

private sealed interface Outcome<out T> {
    class Success<T>(val value: T) : Outcome<T>
    class Failure(val error: NetworkException) : Outcome<Nothing>
}
