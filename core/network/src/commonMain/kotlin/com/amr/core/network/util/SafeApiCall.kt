package com.amr.core.network.util

import com.amr.core.network.exception.NetworkException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Executes [apiCall], validates the HTTP status, and parses the body into [T].
 * Parsing happens inside the same error boundary, so network, timeout, and
 * serialization failures are all surfaced as [NetworkException].
 */
suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> HttpResponse
): T {
    try {
        val response = apiCall()

        if (!response.status.isSuccess()) {
            throw when (response.status.value) {
                401 -> NetworkException.Unauthorized()
                404 -> NetworkException.NotFound()

                in 500..599 -> NetworkException.ServerError()

                else -> NetworkException.HttpError(
                    response.status.value
                )
            }
        }

        return response.body<T>()

    } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
            throw NetworkException.Timeout()
        }

        throw e

    } catch (e: HttpRequestTimeoutException) {
        throw NetworkException.Timeout()

    } catch (e: ConnectTimeoutException) {
        throw NetworkException.Timeout()

    } catch (e: SocketTimeoutException) {
        throw NetworkException.Timeout()

    } catch (e: ContentConvertException) {
        throw NetworkException.Serialization()

    } catch (e: SerializationException) {
        throw NetworkException.Serialization()

    } catch (e: IOException) {
        throw NetworkException.NoInternet()
    }
}
