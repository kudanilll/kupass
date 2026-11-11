package com.nielcode.kupass.data.local.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferenceManagerTest {

    @Test
    fun `excludeLookalike defaults to false and persists updates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)

        assertFalse(prefs.excludeLookalike)

        prefs.excludeLookalike = true
        assertTrue(prefs.excludeLookalike)

        val secondPrefs = PreferenceManager(context)
        assertTrue(secondPrefs.excludeLookalike)

        prefs.excludeLookalike = false
        assertFalse(prefs.excludeLookalike)
    }

    @Test
    fun `offer to save logins defaults to on and persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)

        assertTrue(prefs.offerToSaveLogins)

        prefs.offerToSaveLogins = false
        assertFalse(PreferenceManager(context).offerToSaveLogins)
    }

    @Test
    fun `the update check defaults to on and persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)

        assertTrue(prefs.checkForUpdates)

        prefs.checkForUpdates = false
        assertFalse(PreferenceManager(context).checkForUpdates)
    }
}
