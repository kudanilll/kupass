package com.nielcode.kupass.data.siteicon

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavgetIconSourceTest {

    private lateinit var server: HttpServer
    private val requests = mutableListOf<HttpExchange>()
    private var respond: (HttpExchange) -> Unit = { it.sendEmpty(500) }

    private val baseUrl
        get() = "http://127.0.0.1:${server.address.port}"

    private val source
        get() = FavgetIconSource(baseUrl, API_KEY)

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            synchronized(requests) { requests += exchange }
            respond(exchange)
        }
        server.start()
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `sends only the domain, with the key in the header`() {
        respond = { it.sendEmpty(404) }

        source.fetch("github.com")

        val request = requests.single()
        assertEquals("/v1/icon", request.requestURI.path)
        assertEquals("domain=github.com", request.requestURI.rawQuery)
        assertEquals("Bearer $API_KEY", request.requestHeaders.getFirst("Authorization"))
    }

    @Test
    fun `returns the image when the api answers directly`() {
        val png = byteArrayOf(1, 2, 3)
        respond = { it.send(200, "image/png", png) }

        val result = source.fetch("github.com")

        assertArrayEquals(png, (result as IconFetchResult.Found).bytes)
    }

    @Test
    fun `refuses to follow a redirect that is not https`() {
        respond = { exchange ->
            exchange.responseHeaders.add("Location", "$baseUrl/cdn/icon.png")
            exchange.sendEmpty(302)
        }

        val result = source.fetch("github.com")

        assertEquals(IconFetchResult.Failed, result)
        // The key never reaches the redirect target: it isn't even requested.
        assertEquals(1, requests.size)
    }

    @Test
    fun `maps missing icons and server trouble differently`() {
        respond = { it.sendEmpty(404) }
        assertEquals(IconFetchResult.NotFound, source.fetch("github.com"))

        respond = { it.sendEmpty(401) }
        assertEquals(IconFetchResult.Failed, source.fetch("github.com"))

        respond = { it.sendEmpty(429) }
        assertEquals(IconFetchResult.Failed, source.fetch("github.com"))

        respond = { it.sendEmpty(503) }
        assertEquals(IconFetchResult.Failed, source.fetch("github.com"))
    }

    @Test
    fun `rejects responses that are not images`() {
        respond = { it.send(200, "text/html", "<html>".toByteArray()) }

        assertEquals(IconFetchResult.NotFound, source.fetch("github.com"))
    }

    @Test
    fun `fails cleanly when the server is unreachable`() {
        val unreachable = FavgetIconSource("http://127.0.0.1:1", API_KEY)

        assertEquals(IconFetchResult.Failed, unreachable.fetch("github.com"))
    }

    @Test
    fun `reads streams only up to the limit`() {
        assertArrayEquals(ByteArray(4), ByteArrayInputStream(ByteArray(4)).readAtMost(4))
        assertNull(ByteArrayInputStream(ByteArray(5)).readAtMost(4))
        assertTrue(ByteArrayInputStream(ByteArray(0)).readAtMost(4)!!.isEmpty())
    }

    private fun HttpExchange.sendEmpty(code: Int) {
        sendResponseHeaders(code, -1)
        close()
    }

    private fun HttpExchange.send(code: Int, contentType: String, body: ByteArray) {
        responseHeaders.add("Content-Type", contentType)
        sendResponseHeaders(code, body.size.toLong())
        responseBody.use { it.write(body) }
    }

    private companion object {
        const val API_KEY = "test-key"
    }
}
