package com.nielcode.kupass.update

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseSourceTest {

    private val server =
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    private var status = 200
    private var body = "[]"
    private var requestHeaders: Headers? = null
    private var requests = 0

    init {
        server.createContext("/releases") { exchange ->
            requests++
            requestHeaders = exchange.requestHeaders
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    private val feedUrl = "http://127.0.0.1:${server.address.port}/releases"

    private var running = true

    @After
    fun stop() {
        if (running) server.stop(0)
    }

    @Test
    fun `the release list is returned and only a generic User-Agent is sent`() {
        body =
            """[{"tag_name": "v3.1.0-beta.2", "html_url": "https://example.com", "assets": []}]"""

        assertEquals(listOf("v3.1.0-beta.2"), fetchReleases(feedUrl)?.map { it.tagName })
        val headers = requireNotNull(requestHeaders)
        assertEquals("Kupass", headers.getFirst("User-Agent"))
        assertEquals("application/vnd.github+json", headers.getFirst("Accept"))
        assertNull(headers.getFirst("Cookie"))
        assertNull(headers.getFirst("Authorization"))
    }

    @Test
    fun `an error status or a body that is not a release list is a failure`() {
        status = 403
        body = """{"message": "API rate limit exceeded"}"""
        assertNull(fetchReleases(feedUrl))

        status = 200
        body = "<html>"
        assertNull(fetchReleases(feedUrl))
    }

    @Test
    fun `a body over 1 MiB is not read into memory`() {
        body = "[" + " ".repeat(1024 * 1024) + "]"

        assertNull(fetchReleases(feedUrl))
    }

    @Test
    fun `a feed that is not HTTPS is never requested, except on this device`() {
        assertNull(fetchReleases("http://example.com/releases"))
        assertNull(fetchReleases("not a url"))
        assertEquals(0, requests)
    }

    @Test
    fun `an unreachable feed is a failure`() {
        server.stop(0)
        running = false

        assertNull(fetchReleases(feedUrl))
    }
}
