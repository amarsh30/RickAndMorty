package com.amr.core.data.remote.datasource

import com.amr.core.network.ApiConfig
import com.amr.core.network.exception.NetworkException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Exercises [CharacterRemoteDataSourceImpl] together with `safeApiCall`
 * against a [MockEngine], so status validation, parsing and error mapping
 * run through the real production code path.
 */
class CharacterRemoteDataSourceImplTest {

    private val jsonHeaders = headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString()
    )

    // Mirrors the configuration in HttpClientFactory, which does not accept an engine.
    private fun createDataSource(handler: MockRequestHandler): CharacterRemoteDataSourceImpl {
        val client = HttpClient(MockEngine(handler)) {
            defaultRequest {
                url(ApiConfig.BASE_URL)
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }

        return CharacterRemoteDataSourceImpl(client)
    }

    @Test
    fun getCharacters_when200WithValidJson_returnsParsedDto() = runTest {
        val dataSource = createDataSource { request ->
            assertEquals("/api/character", request.url.encodedPath)

            respond(
                content = """
                    {
                      "info": { "count": 1 },
                      "results": [
                        {
                          "id": 1,
                          "name": "Rick Sanchez",
                          "status": "Alive",
                          "species": "Human",
                          "gender": "Male",
                          "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val response = dataSource.getCharacters()

        assertEquals(1, response.results.size)

        val character = response.results.first()
        assertEquals(1, character.id)
        assertEquals("Rick Sanchez", character.name)
        assertEquals("Alive", character.status)
        assertEquals("Human", character.species)
        assertEquals(
            "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
            character.imageUrl
        )
    }

    @Test
    fun getCharacters_when401_throwsUnauthorized() = runTest {
        val dataSource = createDataSource {
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        assertFailsWith<NetworkException.Unauthorized> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_when500_throwsServerError() = runTest {
        val dataSource = createDataSource {
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }

        assertFailsWith<NetworkException.ServerError> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_when200WithInvalidJson_throwsSerialization() = runTest {
        val dataSource = createDataSource {
            // Required fields (name, status, species, image) are missing.
            respond(
                content = """{ "results": [ { "id": 1 } ] }""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        assertFailsWith<NetworkException.Serialization> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenCancelled_propagatesCancellationException() = runTest {
        val requestStarted = CompletableDeferred<Unit>()

        val dataSource = createDataSource {
            requestStarted.complete(Unit)
            awaitCancellation()
        }

        var thrown: Throwable? = null

        val job = launch {
            try {
                dataSource.getCharacters()
            } catch (e: Throwable) {
                // Record what escaped getCharacters(), then rethrow to keep cancellation intact.
                thrown = e
                throw e
            }
        }

        requestStarted.await()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertIs<CancellationException>(thrown)
        assertTrue(thrown !is NetworkException)
    }
}
