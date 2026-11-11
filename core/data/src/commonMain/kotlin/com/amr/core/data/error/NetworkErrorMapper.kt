package com.amr.core.data.error

import com.amr.core.domain.error.DataError
import com.amr.core.network.exception.NetworkException

/**
 * Translates a transport-level [NetworkException] into a domain [DataError],
 * keeping the original exception as the cause.
 */
fun NetworkException.toDataError(): DataError {
    return when (this) {
        is NetworkException.NoInternet -> DataError.NoConnection(this)
        is NetworkException.Timeout -> DataError.Timeout(this)
        is NetworkException.Unauthorized -> DataError.Unauthorized(this)
        is NetworkException.NotFound -> DataError.NotFound(this)
        is NetworkException.ServerError -> DataError.Server(this)
        // The server is up but refusing work for now: shown like a server problem.
        is NetworkException.TooManyRequests -> DataError.Server(this)
        is NetworkException.Serialization -> DataError.InvalidData(this)
        is NetworkException.HttpError -> DataError.Unknown(this)
    }
}
