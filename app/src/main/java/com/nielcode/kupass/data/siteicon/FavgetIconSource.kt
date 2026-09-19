package com.nielcode.kupass.data.siteicon

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Outcome of asking an [IconSource] for a domain's icon. */
sealed interface IconFetchResult {
    /** Raw image bytes, not yet decoded or validated as an image. */
    class Found(val bytes: ByteArray) : IconFetchResult

    /** The site has no icon (or it isn't a usable image). Worth remembering for a while. */
    data object NotFound : IconFetchResult

    /** Network or server trouble. Worth retrying later. */
    data object Failed : IconFetchResult
}

/** Blocking icon lookup by domain; callers run it on an IO dispatcher. */
fun interface IconSource {
    fun fetch(domain: String): IconFetchResult
}

/**
 * Fetches icons from the Favget API (`GET /v1/icon?domain=`), which answers with a redirect to a
 * CDN image.
 *
 * Only the domain is sent. The API key goes to the Favget host alone: redirects are not followed
 * automatically, and the redirected image is fetched over HTTPS without the key. Nothing is logged.
 */
class FavgetIconSource(private val baseUrl: String, private val apiKey: String) : IconSource {

    override fun fetch(domain: String): IconFetchResult {
        val endpoint =
            try {
                URL("$baseUrl/v1/icon?domain=${URLEncoder.encode(domain, "UTF-8")}")
            } catch (_: IOException) {
                return IconFetchResult.Failed
            }
        return request(endpoint, authorized = true) { connection ->
            when (val code = connection.responseCode) {
                in RedirectCodes -> fetchRedirect(endpoint, connection.getHeaderField("Location"))
                HttpURLConnection.HTTP_OK -> readImage(connection)
                else -> failureFor(code)
            }
        }
    }

    private fun fetchRedirect(endpoint: URL, location: String?): IconFetchResult {
        val target =
            try {
                location?.let { URL(endpoint, it) }
            } catch (_: IOException) {
                null
            }
        if (target?.protocol != "https") return IconFetchResult.Failed
        return request(target, authorized = false) { connection ->
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> readImage(connection)
                else -> failureFor(code)
            }
        }
    }

    private inline fun request(
        url: URL,
        authorized: Boolean,
        handle: (HttpURLConnection) -> IconFetchResult,
    ): IconFetchResult {
        val connection =
            try {
                url.openConnection() as HttpURLConnection
            } catch (_: IOException) {
                return IconFetchResult.Failed
            }
        return try {
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", ACCEPT_IMAGES)
            if (authorized) connection.setRequestProperty("Authorization", "Bearer $apiKey")
            handle(connection)
        } catch (_: IOException) {
            IconFetchResult.Failed
        } finally {
            connection.disconnect()
        }
    }

    private fun readImage(connection: HttpURLConnection): IconFetchResult {
        val type = connection.contentType.orEmpty()
        if (!type.startsWith("image/") || connection.contentLengthLong > MAX_ICON_BYTES) {
            return IconFetchResult.NotFound
        }
        val bytes = connection.inputStream.use { it.readAtMost(MAX_ICON_BYTES) }
        return bytes?.let(IconFetchResult::Found) ?: IconFetchResult.NotFound
    }

    /** Auth and rate-limit errors are our problem, not the site's: retry them sooner. */
    private fun failureFor(code: Int): IconFetchResult =
        if (code in ClientErrors && code !in RetryableClientErrors) IconFetchResult.NotFound
        else IconFetchResult.Failed

    private companion object {
        const val TIMEOUT_MILLIS = 8_000
        const val MAX_ICON_BYTES = 256 * 1024
        const val ACCEPT_IMAGES = "image/webp,image/png;q=0.9,image/*;q=0.8"
        val RedirectCodes = setOf(301, 302, 303, 307, 308)
        val ClientErrors = 400..499
        val RetryableClientErrors = setOf(401, 403, 429)
    }
}

/** Reads the whole stream, or returns null if it is longer than [limit] bytes. */
internal fun InputStream.readAtMost(limit: Int): ByteArray? {
    // InputStream.readNBytes needs API 33; minSdk is lower.
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read < 0) return out.toByteArray()
        if (out.size() + read > limit) return null
        out.write(buffer, 0, read)
    }
}
