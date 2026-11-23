package net.eidas2sandkasse.tsl

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.atomic.AtomicInteger

/** Serves status lists for any uri without network. Kotlin, since MockEngine takes a suspend handler. */
class MockStatusListEndpoint {

    private class Response(val contentType: String, val body: ByteArray, val cacheControl: String?)

    private val responses = mutableMapOf<String, Response>()
    private val requests = AtomicInteger()

    @JvmOverloads
    fun serve(uri: String, contentType: String, body: ByteArray, cacheControl: String? = null): MockStatusListEndpoint {
        responses[uri] = Response(contentType, body, cacheControl)
        return this
    }

    fun requestCount(): Int = requests.get()

    fun engine(): HttpClientEngine = MockEngine { request ->
        requests.incrementAndGet()
        val response = responses[request.url.toString()]
            ?: return@MockEngine respond("", HttpStatusCode.NotFound)
        val headers = listOfNotNull(
            HttpHeaders.ContentType to listOf(response.contentType),
            response.cacheControl?.let { HttpHeaders.CacheControl to listOf(it) },
        )
        respond(response.body, HttpStatusCode.OK, headersOf(*headers.toTypedArray()))
    }
}
