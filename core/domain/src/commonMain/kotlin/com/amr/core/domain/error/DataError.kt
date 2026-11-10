package com.amr.core.domain.error

/**
 * Failures that the data layer may surface to the domain and presentation layers.
 *
 * Subtypes describe what went wrong in business terms, independent of the
 * transport or storage implementation. [cause] keeps the original exception
 * for debugging and logging only; callers should branch on the subtype.
 */
sealed class DataError(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class NoConnection(cause: Throwable? = null) :
        DataError("No connection", cause)

    class Timeout(cause: Throwable? = null) :
        DataError("Request timed out", cause)

    class Unauthorized(cause: Throwable? = null) :
        DataError("Unauthorized", cause)

    class NotFound(cause: Throwable? = null) :
        DataError("Data not found", cause)

    class Server(cause: Throwable? = null) :
        DataError("Server error", cause)

    class InvalidData(cause: Throwable? = null) :
        DataError("Invalid data", cause)

    class Unknown(cause: Throwable? = null) :
        DataError("Unknown error", cause)
}
