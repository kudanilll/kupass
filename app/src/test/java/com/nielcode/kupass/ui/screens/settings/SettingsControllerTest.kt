package com.nielcode.kupass.ui.screens.settings

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.update.AppVersion
import com.nielcode.kupass.update.AvailableUpdate
import com.nielcode.kupass.update.CheckResult
import com.nielcode.kupass.utils.AppConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsControllerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val appDynamicColor = mutableStateOf(true)

    private fun settings(checkNow: suspend () -> CheckResult = { CheckResult.UpToDate }) =
        SettingsController(context, PreferenceManager(context), appDynamicColor, checkNow)

    @Test
    fun `dynamic colors switch the open app at once and persist`() {
        val settings = settings()

        settings.selectDynamicColor(false)

        assertFalse(settings.dynamicColor)
        assertFalse(appDynamicColor.value)
        assertEquals(
            AppConfig.DynamicColors.Code.DISABLE,
            PreferenceManager(context).dynamicColor,
        )
    }

    @Test
    fun `offer to save toggle defaults to on and persists`() {
        val settings = settings()

        assertTrue(settings.offerToSaveLogins)

        settings.selectOfferToSaveLogins(false)

        assertFalse(settings.offerToSaveLogins)
        assertFalse(PreferenceManager(context).offerToSaveLogins)
    }

    @Test
    fun `Check for updates says what it found, unless the update dialog opens`() = runTest {
        var result: CheckResult = CheckResult.UpToDate
        val settings = settings(checkNow = { result })

        assertEquals(R.string.update_check_latest, settings.checkForUpdates())
        result = CheckResult.Failed
        assertEquals(R.string.update_check_failed, settings.checkForUpdates())
        result = CheckResult.Unavailable
        assertEquals(R.string.update_check_release_only, settings.checkForUpdates())
        result = CheckResult.Found(UPDATE)
        assertNull(settings.checkForUpdates())
        assertFalse(settings.checkingForUpdates)
    }

    @Test
    fun `while it checks, the row says so and another tap starts nothing`() = runTest {
        val answer = CompletableDeferred<CheckResult>()
        var checks = 0
        val settings =
            settings(
                checkNow = {
                    checks++
                    answer.await()
                }
            )

        val first = async { settings.checkForUpdates() }
        runCurrent()
        assertTrue(settings.checkingForUpdates)
        assertNull(settings.checkForUpdates())

        answer.complete(CheckResult.UpToDate)
        assertEquals(R.string.update_check_latest, first.await())
        assertFalse(settings.checkingForUpdates)
        assertEquals(1, checks)
    }

    private companion object {
        val UPDATE =
            AvailableUpdate(
                version = AppVersion(3, 1, 0, beta = 3),
                tag = "v3.1.0-beta.3",
                apkUrl = "https://example.com/kupass.apk",
                apkSize = 10,
                checksumUrl = "https://example.com/kupass.apk.sha256",
                pageUrl = "https://example.com/release",
            )
    }
}
