package com.amr.core.network.util

import com.amr.core.network.exception.NetworkException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException

suspend fun safeApiCall(
    apiCall: suspend () -> HttpResponse
): HttpResponse {
    try {
        val response = apiCall()

        if (response.status.isSuccess()) {
            return response
        }

        throw when (response.status.value) {
            401 -> NetworkException.Unauthorized()
            404 -> NetworkException.NotFound()

            in 500..599 -> NetworkException.ServerError()

            else -> NetworkException.HttpError(
                response.status.value
            )
        }

    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
            throw NetworkException.Timeout()
        }

        throw e

    } catch (e: HttpRequestTimeoutException) {
        throw NetworkException.Timeout()

    } catch (e: IOException) {
        throw NetworkException.NoInternet()
    }
}