package com.amr.core.data.remote.datasource

import com.amr.core.network.ApiConfig
import com.amr.core.network.exception.NetworkException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpResponseData
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
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
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

    private val rickJson = """
        {
          "id": 1,
          "name": "Rick Sanchez",
          "status": "Alive",
          "species": "Human",
          "type": "",
          "gender": "Male",
          "origin": { "name": "Earth (C-137)", "url": "https://rickandmortyapi.com/api/location/1" },
          "location": { "name": "Citadel of Ricks", "url": "https://rickandmortyapi.com/api/location/3" },
          "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
          "episode": [
            "https://rickandmortyapi.com/api/episode/1",
            "https://rickandmortyapi.com/api/episode/2"
          ],
          "url": "https://rickandmortyapi.com/api/character/1",
          "created": "2017-11-04T18:48:46.250Z"
        }
    """.trimIndent()

    @Test
    fun searchCharacters_sendsOnlyNonNullFiltersAsQueryParameters() = runTest {
        var requestUrl: String? = null
        val dataSource = createDataSource { request ->
            requestUrl = request.url.toString()
            respondPageJson()
        }

        dataSource.searchCharacters(name = "rick sanchez", status = "alive", species = null, gender = "male")

        assertEquals(
            "https://rickandmortyapi.com/api/character?name=rick+sanchez&status=alive&gender=male",
            requestUrl
        )
    }

    @Test
    fun searchCharacters_whenNothingMatches_throwsNotFound() = runTest {
        val dataSource = createDataSource {
            respond(
                content = """{ "error": "There is nothing here" }""",
                status = HttpStatusCode.NotFound,
                headers = jsonHeaders
            )
        }

        assertFailsWith<NetworkException.NotFound> {
            dataSource.searchCharacters(name = "zzz", status = null, species = null, gender = null)
        }
    }

    @Test
    fun getCharacter_requestsCharacterPathAndParsesDetail() = runTest {
        var requestPath: String? = null
        val dataSource = createDataSource { request ->
            requestPath = request.url.encodedPath
            respond(content = rickJson, status = HttpStatusCode.OK, headers = jsonHeaders)
        }

        val dto = dataSource.getCharacter(1)

        assertEquals("/api/character/1", requestPath)
        assertEquals("Male", dto.gender)
        assertEquals("https://rickandmortyapi.com/api/location/3", dto.location.url)
        assertEquals(2, dto.episode.size)
    }

    /** Builds a list response shaped like the real API, with [infoJson] as the `info` object. */
    private fun pageJson(infoJson: String): String = """
        {
          "info": $infoJson,
          "results": [ $rickJson ]
        }
    """.trimIndent()

    private val firstPageInfo = """
        {
          "count": 826,
          "pages": 42,
          "next": "https://rickandmortyapi.com/api/character?page=2",
          "prev": null
        }
    """.trimIndent()

    private fun MockRequestHandleScope.respondPageJson(
        infoJson: String = firstPageInfo
    ): HttpResponseData = respond(
        content = pageJson(infoJson),
        status = HttpStatusCode.OK,
        headers = jsonHeaders
    )

    private fun respondPage(infoJson: String = firstPageInfo): MockRequestHandler = {
        respondPageJson(infoJson)
    }

    @Test
    fun getCharacters_withoutPage_requestsFirstPage() = runTest {
        var requestedPage: String? = null
        val dataSource = createDataSource { request ->
            assertEquals("/api/character", request.url.encodedPath)
            requestedPage = request.url.parameters["page"]
            respondPageJson()
        }

        dataSource.getCharacters()

        assertEquals("1", requestedPage)
    }

    @Test
    fun getCharacters_withPage2_sendsPageQueryParameter() = runTest {
        var requestedPage: String? = null
        var requestUrl: String? = null
        val dataSource = createDataSource { request ->
            requestedPage = request.url.parameters["page"]
            requestUrl = request.url.toString()
            respondPageJson()
        }

        dataSource.getCharacters(page = 2)

        assertEquals("2", requestedPage)
        assertEquals("https://rickandmortyapi.com/api/character?page=2", requestUrl)
    }

    @Test
    fun getCharacters_firstPage_parsesInfoWithNextAndNullPrev() = runTest {
        val dataSource = createDataSource(respondPage(firstPageInfo))

        val info = dataSource.getCharacters().info

        assertEquals(826, info.count)
        assertEquals(42, info.pages)
        assertEquals("https://rickandmortyapi.com/api/character?page=2", info.next)
        assertNull(info.prev)
    }

    @Test
    fun getCharacters_lastPage_parsesNullNext() = runTest {
        val lastPageInfo = """
            {
              "count": 826,
              "pages": 42,
              "next": null,
              "prev": "https://rickandmortyapi.com/api/character?page=41"
            }
        """.trimIndent()
        val dataSource = createDataSource(respondPage(lastPageInfo))

        val info = dataSource.getCharacters(page = 42).info

        assertNull(info.next)
        assertEquals("https://rickandmortyapi.com/api/character?page=41", info.prev)
    }

    @Test
    fun getCharacters_whenInfoMissing_throwsSerialization() = runTest {
        val dataSource = createDataSource {
            respond(
                content = """{ "results": [ $rickJson ] }""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        assertFailsWith<NetworkException.Serialization> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenRequiredInfoFieldMissing_throwsSerialization() = runTest {
        // "pages" is missing.
        val dataSource = createDataSource(
            respondPage("""{ "count": 826, "next": null, "prev": null }""")
        )

        assertFailsWith<NetworkException.Serialization> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenInfoFieldHasInvalidType_throwsSerialization() = runTest {
        val dataSource = createDataSource(
            respondPage("""{ "count": 826, "pages": "many", "next": null, "prev": null }""")
        )

        assertFailsWith<NetworkException.Serialization> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_when200WithValidJson_returnsParsedDto() = runTest {
        val dataSource = createDataSource { request ->
            assertEquals("/api/character", request.url.encodedPath)

            respondPageJson()
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
            // Required character fields (name, status, species, image) are missing.
            respond(
                content = """{ "info": $firstPageInfo, "results": [ { "id": 1 } ] }""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        assertFailsWith<NetworkException.Serialization> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenIOException_throwsNoInternet() = runTest {
        val dataSource = createDataSource {
            throw IOException("offline")
        }

        assertFailsWith<NetworkException.NoInternet> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenConnectTimeout_throwsTimeout() = runTest {
        val dataSource = createDataSource {
            throw ConnectTimeoutException("connect timeout")
        }

        assertFailsWith<NetworkException.Timeout> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenSocketTimeout_throwsTimeout() = runTest {
        val dataSource = createDataSource {
            throw SocketTimeoutException("socket timeout")
        }

        assertFailsWith<NetworkException.Timeout> {
            dataSource.getCharacters()
        }
    }

    @Test
    fun getCharacters_whenRequestTimeout_throwsTimeout() = runTest {
        val dataSource = createDataSource {
            throw HttpRequestTimeoutException("${ApiConfig.BASE_URL}character", 15_000)
        }

        assertFailsWith<NetworkException.Timeout> {
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
