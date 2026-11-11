package com.nielcode.kupass.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    private val prefs = PreferenceManager(ApplicationProvider.getApplicationContext<Context>())
    private val installed = AppVersion(3, 1, 0, beta = 1)
    private var feed: List<GitHubRelease>? = listOf(release("v3.1.0-beta.2"))
    private var requests = 0

    private fun checker(version: AppVersion? = installed) =
        UpdateChecker(
            prefs,
            version,
            fetchReleases = {
                requests++
                feed
            },
        )

    @Test
    fun `every open asks for the release list and offers the newest release`() {
        assertEquals("v3.1.0-beta.2", checker().updateToOffer()?.tag)

        feed = listOf(release("v3.1.0-beta.3"), release("v3.1.0-beta.2"))
        assertEquals("v3.1.0-beta.3", checker().updateToOffer()?.tag)
        assertEquals(2, requests)
    }

    @Test
    fun `a failed request or nothing newer offers nothing`() {
        feed = null
        assertNull(checker().updateToOffer())

        feed = emptyList()
        assertNull(checker().updateToOffer())

        feed = listOf(release("v3.1.0-beta.1"))
        assertNull(checker().updateToOffer())
    }

    @Test
    fun `nothing is asked or offered with the switch off or in a debug build`() {
        prefs.checkForUpdates = false
        assertNull(checker().updateToOffer())

        prefs.checkForUpdates = true
        assertNull(checker(version = null).updateToOffer())
        assertEquals(0, requests)
    }

    private companion object {
        fun release(tag: String) =
            GitHubRelease(
                tagName = tag,
                pageUrl = "https://github.com/kudanilll/kupass/releases/tag/$tag",
                assets =
                    listOf(
                        GitHubAsset("kupass-$tag.apk", "https://example.com/$tag.apk", 10),
                        GitHubAsset("kupass-$tag.apk.sha256", "https://example.com/$tag.sha", 70),
                    ),
            )
    }
}
