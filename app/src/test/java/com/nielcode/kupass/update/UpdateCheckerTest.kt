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
    private var clock = START
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
            now = { clock },
        )

    @Test
    fun `the first open asks for the release list and offers the newer release`() {
        assertEquals("v3.1.0-beta.2", checker().updateToOffer()?.tag)
        assertEquals(1, requests)
        assertEquals(START, prefs.lastUpdateCheckMillis)
    }

    @Test
    fun `within a day the release list is not asked again, and the found release stays offered`() {
        checker().updateToOffer()
        clock += DAY - 1
        feed = emptyList()

        assertEquals("v3.1.0-beta.2", checker().updateToOffer()?.tag)
        assertEquals(1, requests)
    }

    @Test
    fun `after a day the release list is asked again and replaces what was found`() {
        checker().updateToOffer()
        clock += DAY
        feed = listOf(release("v3.1.0-beta.3"), release("v3.1.0-beta.2"))
        assertEquals("v3.1.0-beta.3", checker().updateToOffer()?.tag)

        clock += DAY
        feed = emptyList()
        assertNull(checker().updateToOffer())
        assertEquals(3, requests)
    }

    @Test
    fun `a failed request waits a day too and keeps the release found before`() {
        checker().updateToOffer()
        clock += DAY
        feed = null

        assertEquals("v3.1.0-beta.2", checker().updateToOffer()?.tag)
        assertEquals(clock, prefs.lastUpdateCheckMillis)
        clock += 1
        checker().updateToOffer()
        assertEquals(2, requests)
    }

    @Test
    fun `a clock set back asks again instead of waiting`() {
        checker().updateToOffer()
        clock -= 1

        checker().updateToOffer()
        assertEquals(2, requests)
    }

    @Test
    fun `a release postponed with Later is not offered again, a newer one is`() {
        val checker = checker()
        checker.skip(requireNotNull(checker.updateToOffer()))
        assertNull(checker.updateToOffer())

        clock += DAY
        feed = listOf(release("v3.1.0-beta.3"))
        assertEquals("v3.1.0-beta.3", checker().updateToOffer()?.tag)
    }

    @Test
    fun `a found release that is already installed is not offered`() {
        checker().updateToOffer()

        assertNull(checker(AppVersion(3, 1, 0, beta = 2)).updateToOffer())
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
        const val START = 1_800_000_000_000L
        const val DAY = 24 * 60 * 60 * 1000L

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
