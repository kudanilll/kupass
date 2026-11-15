package com.nielcode.kupass.ui.theme

import android.app.UiModeManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.utils.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppearanceSettingsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Config(sdk = [33])
    fun `from Android 12 the system gets the theme, so the splash screen draws in it too`() {
        val uiMode = shadowOf(context.getSystemService(UiModeManager::class.java))

        AppearanceSettings.applyTheme(context, AppConfig.Theme.Code.LIGHT)
        assertEquals(UiModeManager.MODE_NIGHT_NO, uiMode.applicationNightMode)
        // AppCompat follows the night mode the system now gives Kupass.
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.getDefaultNightMode(),
        )

        AppearanceSettings.applyTheme(context, AppConfig.Theme.Code.DARK)
        assertEquals(UiModeManager.MODE_NIGHT_YES, uiMode.applicationNightMode)

        AppearanceSettings.applyTheme(context, AppConfig.Theme.Code.SYSTEM)
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, uiMode.applicationNightMode)
    }

    @Test
    @Config(sdk = [30])
    fun `before Android 12 AppCompat applies the theme`() {
        AppearanceSettings.applyTheme(context, AppConfig.Theme.Code.DARK)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        AppearanceSettings.applyTheme(context, AppConfig.Theme.Code.SYSTEM)
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.getDefaultNightMode(),
        )
    }
}
