package com.nielcode.kupass.data.local.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `the update check defaults to on with nothing found, and its state persists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)

        assertTrue(prefs.checkForUpdates)
        assertEquals(0L, prefs.lastUpdateCheckMillis)
        assertNull(prefs.pendingUpdate)
        assertNull(prefs.skippedUpdateTag)

        prefs.checkForUpdates = false
        prefs.lastUpdateCheckMillis = 1_700_000_000_000L
        prefs.pendingUpdate = "{}"
        prefs.skippedUpdateTag = "v3.1.0-beta.2"
        val reopened = PreferenceManager(context)
        assertFalse(reopened.checkForUpdates)
        assertEquals(1_700_000_000_000L, reopened.lastUpdateCheckMillis)
        assertEquals("{}", reopened.pendingUpdate)
        assertEquals("v3.1.0-beta.2", reopened.skippedUpdateTag)

        prefs.pendingUpdate = null
        assertNull(PreferenceManager(context).pendingUpdate)
    }
}
