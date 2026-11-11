package com.nielcode.kupass.update

import com.nielcode.kupass.data.siteicon.readAtMost
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Requests the release list from [feedUrl] (the GitHub Releases API). Sends only a generic
 * `User-Agent: Kupass`; nothing about the device, the build, or the vault. Blocking; null on any
 * failure.
 */
internal fun fetchReleases(feedUrl: String): List<GitHubRelease>? =
    if (!isAllowedUrl(feedUrl)) null
    else
        httpGet(feedUrl, accept = "application/vnd.github+json") { it.readAtMost(MAX_FEED_BYTES) }
            ?.let { parseReleases(it.decodeToString()) }

/**
 * GET [url] for the update flow, sending only `User-Agent: Kupass` (and [accept]). Redirects are
 * followed within the same protocol, as GitHub's asset links need; one that switches protocols ends
 * as a non-200 response. [read] runs on a 200 response only. Null on any failure.
 */
internal inline fun <T> httpGet(
    url: String,
    accept: String? = null,
    read: (InputStream) -> T?,
): T? =
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            accept?.let { connection.setRequestProperty("Accept", it) }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use(read)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (_: IOException) {
        null
    }

internal const val USER_AGENT = "Kupass"
internal const val TIMEOUT_MILLIS = 15_000
private const val MAX_FEED_BYTES = 1024 * 1024
