package com.nielcode.kupass.ui.screens.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsControllerTest {

    @Test
    fun `offer to save toggle defaults to on and persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsController(context, PreferenceManager(context))

        assertTrue(settings.offerToSaveLogins)

        settings.selectOfferToSaveLogins(false)

        assertFalse(settings.offerToSaveLogins)
        assertFalse(PreferenceManager(context).offerToSaveLogins)
    }

    @Test
    fun `turning the update check off persists and forgets the update it found`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)
        prefs.pendingUpdate = "{}"
        val settings = SettingsController(context, prefs)

        assertTrue(settings.checkForUpdates)

        settings.selectCheckForUpdates(false)

        assertFalse(settings.checkForUpdates)
        assertFalse(PreferenceManager(context).checkForUpdates)
        assertNull(PreferenceManager(context).pendingUpdate)
    }
}
