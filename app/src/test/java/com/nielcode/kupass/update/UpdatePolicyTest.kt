package com.nielcode.kupass.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdatePolicyTest {

    private val stable = AppVersion(3, 1, 0)
    private val beta1 = AppVersion(3, 1, 0, beta = 1)

    @Test
    fun `the GitHub release list is parsed and unknown fields are ignored`() {
        val releases = requireNotNull(parseReleases(FEED))

        assertEquals(
            listOf("v3.1.1", "v3.1.0-beta.3", "v3.1.0-beta.2"),
            releases.map { it.tagName },
        )
        assertEquals(
            "https://github.com/kudanilll/kupass/releases/download/v3.1.0-beta.3/kupass-v3.1.0-beta.3.apk",
            releases[1].assets[0].url,
        )
        assertEquals(5_242_880L, releases[1].assets[0].size)
    }

    @Test
    fun `anything that is not a release list is refused`() {
        assertNull(parseReleases("""{"message":"API rate limit exceeded"}"""))
        assertNull(parseReleases("<html>"))
    }

    @Test
    fun `a beta build is offered the newest release, beta or stable`() {
        val update = requireNotNull(newestUpdate(releases(BETA_3, BETA_2), beta1))

        assertEquals(AppVersion(3, 1, 0, beta = 3), update.version)
        assertEquals("v3.1.0-beta.3", update.tag)
        assertEquals(apkUrl("v3.1.0-beta.3"), update.apkUrl)
        assertEquals(5_242_880L, update.apkSize)
        assertEquals("${apkUrl("v3.1.0-beta.3")}.sha256", update.checksumUrl)
        assertEquals(pageUrl("v3.1.0-beta.3"), update.pageUrl)

        assertEquals("v3.1.1", newestUpdate(releases(STABLE_311, BETA_3), beta1)?.tag)
    }

    @Test
    fun `a stable build is never offered a beta`() {
        assertNull(newestUpdate(releases(BETA_3, BETA_2), stable))
        assertEquals("v3.1.1", newestUpdate(releases(BETA_3, STABLE_311), stable)?.tag)
        // A beta tag counts as a beta even when the release isn't marked as a prerelease.
        assertNull(
            newestUpdate(
                releases(BETA_3.replace("\"prerelease\": true", "\"prerelease\": false")),
                stable,
            )
        )
    }

    @Test
    fun `nothing is offered when the build is already the newest`() {
        assertNull(newestUpdate(releases(BETA_2), AppVersion(3, 1, 0, beta = 2)))
        assertNull(newestUpdate(releases(BETA_3), stable))
        assertNull(newestUpdate(emptyList(), beta1))
    }

    @Test
    fun `drafts, odd tags, and releases without both files are skipped`() {
        assertNull(
            newestUpdate(releases(BETA_3.replace("\"draft\": false", "\"draft\": true")), beta1)
        )
        assertNull(newestUpdate(releases(release("v3.1.0-rc.1")), beta1))
        assertNull(newestUpdate(releases(release("v3.1.0-beta.4", checksum = false)), beta1))
        assertNull(newestUpdate(releases(release("v3.1.0-beta.4", apk = false)), beta1))
        // Assets must belong to their own tag.
        assertNull(
            newestUpdate(
                releases(
                    release("v3.1.0-beta.4").replace("kupass-v3.1.0-beta.4", "kupass-v3.1.0-beta.1")
                ),
                beta1,
            )
        )
    }

    @Test
    fun `release links must be HTTPS, or HTTP on this device for testing`() {
        val http = release("v3.1.0-beta.4").replace("https://github.com", "http://github.com")
        assertNull(newestUpdate(releases(http), beta1))

        val local = release("v3.1.0-beta.4").replace("https://github.com", "http://127.0.0.1:8080")
        assertEquals("v3.1.0-beta.4", newestUpdate(releases(local), beta1)?.tag)

        val script =
            release("v3.1.0-beta.4").replace(pageUrl("v3.1.0-beta.4"), "javascript:alert(1)")
        assertNull(newestUpdate(releases(script), beta1))
    }

    private fun releases(vararg items: String): List<GitHubRelease> =
        requireNotNull(parseReleases(items.joinToString(",", "[", "]")))

    private companion object {
        fun apkUrl(tag: String) =
            "https://github.com/kudanilll/kupass/releases/download/$tag/kupass-$tag.apk"

        fun pageUrl(tag: String) = "https://github.com/kudanilll/kupass/releases/tag/$tag"

        fun release(tag: String, apk: Boolean = true, checksum: Boolean = true): String {
            val assets =
                listOfNotNull(
                    """{"name": "kupass-$tag.apk", "size": 5242880, "browser_download_url": "${apkUrl(tag)}", "content_type": "application/vnd.android.package-archive"}"""
                        .takeIf { apk },
                    """{"name": "kupass-$tag.apk.sha256", "size": 89, "browser_download_url": "${apkUrl(tag)}.sha256"}"""
                        .takeIf { checksum },
                )
            return """
                {
                  "id": 1, "tag_name": "$tag", "name": "Kupass $tag",
                  "html_url": "${pageUrl(tag)}",
                  "draft": false, "prerelease": ${tag.contains("beta")},
                  "assets": ${assets.joinToString(",", "[", "]")}
                }
            """
        }

        val STABLE_311 = release("v3.1.1")
        val BETA_3 = release("v3.1.0-beta.3")
        val BETA_2 = release("v3.1.0-beta.2")
        val FEED = "[$STABLE_311, $BETA_3, $BETA_2]"
    }
}
