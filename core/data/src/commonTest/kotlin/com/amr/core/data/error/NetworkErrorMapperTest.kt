package com.amr.core.data.error

import com.amr.core.domain.error.DataError
import com.amr.core.network.exception.NetworkException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class NetworkErrorMapperTest {

    @Test
    fun toDataError_whenNoInternet_returnsNoConnection() {
        val source = NetworkException.NoInternet()
        val result = source.toDataError()

        assertIs<DataError.NoConnection>(result)
        assertSame(source, result.cause)
    }

    @Test
    fun toDataError_whenTimeout_returnsTimeout() {
        val source = NetworkException.Timeout()
        val result = source.toDataError()

        assertIs<DataError.Timeout>(result)
        assertSame(source, result.cause)
    }

    @Test
    fun toDataError_whenUnauthorized_returnsUnauthorized() {
        val source = NetworkException.Unauthorized()
        val result = source.toDataError()

        assertIs<DataError.Unauthorized>(result)
        assertSame(source, result.cause)
    }

    @Test
    fun toDataError_whenNotFound_returnsNotFound() {
        val source = NetworkException.NotFound()
        val result = source.toDataError()

        assertIs<DataError.NotFound>(result)
        assertSame(source, result.cause)
    }

    @Test
    fun toDataError_whenServerError_returnsServer() {
        val source = NetworkException.ServerError()
        val result = source.toDataError()

        assertIs<DataError.Server>(result)
        assertSame(source, result.cause)
    }

    @Test
    fun toDataError_whenSerialization_returnsInvalidData() {
        val source = NetworkException.Serialization()
        val result = source.toDataError()

        assertIs<DataError.InvalidData>(result)
        assertSame(source, result.cause)
    }

    @Test
    fun toDataError_whenHttpErrorWithoutSpecificMapping_returnsUnknown() {
        val source = NetworkException.HttpError(statusCode = 429)
        val result = source.toDataError()

        assertIs<DataError.Unknown>(result)
        assertSame(source, result.cause)
    }
}
