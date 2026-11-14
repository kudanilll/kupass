package com.nielcode.kupass.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.update.CheckResult.Failed
import com.nielcode.kupass.update.CheckResult.Found
import com.nielcode.kupass.update.CheckResult.Unavailable
import com.nielcode.kupass.update.CheckResult.UpToDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    // A new PreferenceManager per checker, as after a restart of the app process.
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val installed = AppVersion(3, 1, 0, beta = 1)
    private var feed: List<GitHubRelease>? = listOf(release("v3.1.0-beta.2"))
    private var requests = 0
    private var now = 1_000_000_000L

    private fun checker(version: AppVersion? = installed) =
        UpdateChecker(
            PreferenceManager(context),
            version,
            fetchReleases = {
                requests++
                feed
            },
            now = { now },
        )

    private fun tagFound(force: Boolean = false, version: AppVersion? = installed): String? =
        (checker(version).check(force) as? Found)?.update?.tag

    @Test
    fun `GitHub is asked at most once an hour`() {
        assertEquals("v3.1.0-beta.2", tagFound())

        feed = listOf(release("v3.1.0-beta.3"))
        now += HOUR - 1
        assertEquals("v3.1.0-beta.2", tagFound())
        assertEquals(1, requests)

        now += 1
        assertEquals("v3.1.0-beta.3", tagFound())
        assertEquals(2, requests)
    }

    @Test
    fun `a release found earlier is offered until it is installed`() {
        checker().check()
        feed = null
        now += HOUR

        // The request fails, and the release found an hour ago is still offered.
        assertEquals("v3.1.0-beta.2", tagFound())
        assertNull(tagFound(version = AppVersion(3, 1, 0, beta = 2)))
    }

    @Test
    fun `Check now always asks`() {
        checker().check()
        feed = listOf(release("v3.1.0-beta.3"))

        assertEquals("v3.1.0-beta.3", tagFound(force = true))
        assertEquals(2, requests)
    }

    @Test
    fun `nothing newer is up to date, and a failed request without a remembered release fails`() {
        feed = listOf(release("v3.1.0-beta.1"))
        assertEquals(UpToDate, checker().check())

        feed = null
        assertEquals(Failed, checker().check(force = true))
    }

    @Test
    fun `a clock set back asks again`() {
        checker().check()
        now -= 1

        checker().check()

        assertEquals(2, requests)
    }

    @Test
    fun `a debug build never asks`() {
        assertEquals(Unavailable, checker(version = null).check(force = true))
        assertEquals(0, requests)
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L

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
