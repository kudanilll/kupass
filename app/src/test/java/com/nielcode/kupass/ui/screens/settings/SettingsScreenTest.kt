package com.nielcode.kupass.ui.screens.settings

import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.App
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.ui.theme.KupassTheme
import com.nielcode.kupass.utils.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsScreenTest {

    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `on and off settings are switches that save at once`() {
        show()

        row(R.string.settings_exclude_lookalike_title).assertIsOff().performClick().assertIsOn()
        row(R.string.settings_offer_save).assertIsOn().performClick().assertIsOff()
        row(R.string.settings_dynamic_colors_title).assertIsOn().performClick().assertIsOff()

        val prefs = PreferenceManager(context)
        assertTrue(prefs.excludeLookalike)
        assertFalse(prefs.offerToSaveLogins)
        assertEquals(AppConfig.DynamicColors.Code.DISABLE, prefs.dynamicColor)
        assertFalse((context as App).container.dynamicColor.value)
    }

    @Test
    @Config(sdk = [30])
    fun `without dynamic colors on the device, its switch is off and says why`() {
        show()

        row(R.string.settings_dynamic_colors_title).assertIsOff().assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.not_supported)).assertExists()
    }

    private fun show() {
        compose.setContent { KupassTheme(dynamicColor = false) { SettingsScreen() } }
    }

    private fun row(title: Int) = compose.onNodeWithText(context.getString(title)).performScrollTo()
}
