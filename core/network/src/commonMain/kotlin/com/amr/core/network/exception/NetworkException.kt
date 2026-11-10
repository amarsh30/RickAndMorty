package com.amr.core.network.exception

sealed class NetworkException(
    message: String
) : Exception(message) {

    class Unauthorized :
        NetworkException("Unauthorized access")

    class NotFound :
        NetworkException("Resource not found")

    class ServerError :
        NetworkException("Server error")

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