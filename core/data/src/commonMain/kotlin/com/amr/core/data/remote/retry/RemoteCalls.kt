package com.amr.core.data.remote.retry

import com.amr.core.domain.model.ApiStatus
import com.amr.core.domain.repository.ApiStatusRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** API health recorded from the final outcome of each remote call. Single instance. */
class ApiStatusTracker : ApiStatusRepository {

    private val status = MutableStateFlow(ApiStatus.Unknown)
    override val apiStatus: StateFlow<ApiStatus> = status.asStateFlow()

    fun recordSuccess() {
        status.value = ApiStatus.Reachable
    }

    fun recordFailure(error: NetworkException) {
        status.value = when (error) {
            // Says nothing about the API; the next request after reconnecting decides.
            is NetworkException.NoInternet -> ApiStatus.Unknown
            // Network or server: a timeout alone cannot tell which.
            is NetworkException.Timeout -> ApiStatus.Unstable
            is NetworkException.ServerError,
            is NetworkException.TooManyRequests,
            is NetworkException.Serialization -> ApiStatus.Degraded
            // The API answered, just not with what was asked for.
            is NetworkException.Unauthorized,
            is NetworkException.NotFound,
            is NetworkException.HttpError -> ApiStatus.Reachable
        }
    }
}

/**
 * Entry point for idempotent GET calls: bounded retry plus API-health recording.
 * Defaults keep repositories constructible in tests without wiring.
 */
class RemoteCalls(
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val apiStatusTracker: ApiStatusTracker? = null
) {
    suspend fun <T> get(block: suspend () -> T): T {
        val result = try {
            retryTransient(retryPolicy, block)
        } catch (error: NetworkException) {
            apiStatusTracker?.recordFailure(error)
            throw error
        }
        apiStatusTracker?.recordSuccess()
        return result
    }
}
