package com.nielcode.kupass.update

import java.net.URI
import java.net.URISyntaxException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** One entry of the GitHub release list; unknown fields are ignored. */
@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val pageUrl: String,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long,
)

/** A newer release this build can install. */
@Serializable
data class AvailableUpdate(
    val version: AppVersion,
    val tag: String,
    val apkUrl: String,
    val apkSize: Long,
    val checksumUrl: String,
    val pageUrl: String,
)

/** Parses the GitHub release list; null if it isn't one. */
internal fun parseReleases(json: String): List<GitHubRelease>? =
    try {
        FeedJson.decodeFromString<List<GitHubRelease>>(json)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

/**
 * The newest release [installed] can update to. Beta builds may move to newer betas or stable
 * releases; stable builds only to stable releases. A beta is recognized by its tag.
 */
internal fun newestUpdate(releases: List<GitHubRelease>, installed: AppVersion): AvailableUpdate? =
    releases
        .asSequence()
        .filterNot { it.draft }
        .mapNotNull { it.toUpdate() }
        .filter { it.version > installed && (installed.beta > 0 || it.version.beta == 0) }
        .maxByOrNull { it.version }

/** HTTPS, or plain HTTP to this device only, so a local feed can be used for device QA. */
internal fun isAllowedUrl(url: String): Boolean {
    val uri =
        try {
            URI(url)
        } catch (_: URISyntaxException) {
            null
        }
    val host = uri?.host
    val scheme = uri?.scheme
    return !host.isNullOrEmpty() &&
        (scheme == "https" || (scheme == "http" && host in LOOPBACK_HOSTS))
}

private fun GitHubRelease.toUpdate(): AvailableUpdate? {
    val version = AppVersion.fromTag(tagName)
    val apk = assets.find { it.name == "kupass-$tagName.apk" }
    val checksum = assets.find { it.name == "kupass-$tagName.apk.sha256" }
    if (version == null || apk == null || checksum == null) return null
    return AvailableUpdate(version, tagName, apk.url, apk.size, checksum.url, pageUrl).takeIf {
        listOf(apk.url, checksum.url, pageUrl).all(::isAllowedUrl)
    }
}

private val FeedJson = Json { ignoreUnknownKeys = true }

private val LOOPBACK_HOSTS = setOf("127.0.0.1", "localhost")
