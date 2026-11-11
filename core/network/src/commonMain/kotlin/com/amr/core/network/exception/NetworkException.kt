package com.amr.core.network.exception

sealed class NetworkException(
    message: String
) : Exception(message) {

    class Unauthorized :
        NetworkException("Unauthorized access")

    class NotFound :
        NetworkException("Resource not found")

    /** A 5xx answer; [statusCode] tells transient gateway errors (502/503/504) from others. */
    class ServerError(
        val statusCode: Int = 500
    ) : NetworkException("Server error: $statusCode")

    /**
     * HTTP 429. [retryAfterMillis] is the wait the server asked for via
     * `Retry-After`, or null when the header is missing or unreadable.
     */
    class TooManyRequests(
        val retryAfterMillis: Long?
    ) : NetworkException("Too many requests")

    class HttpError(
        val statusCode: Int
    ) : NetworkException("HTTP Error: $statusCode")

    class NoInternet :
        NetworkException("No internet connection")

    class Timeout :
        NetworkException("Request timeout")

    class Serialization :
        NetworkException("Invalid response format")
}
