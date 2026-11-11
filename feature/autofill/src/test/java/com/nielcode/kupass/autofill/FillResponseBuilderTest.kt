package com.nielcode.kupass.autofill

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FillResponseBuilderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a newer response keeps the unlock rows of an earlier one working`() {
        val earlier = FillResponseBuilder.authPendingIntent(context, AuthKind.Unlock)
        val later = FillResponseBuilder.authPendingIntent(context, AuthKind.Unlock)

        assertFalse(shadowOf(earlier).isCanceled)
        assertFalse(shadowOf(later).isCanceled)
        assertEquals(AuthKind.Unlock.name, shadowOf(earlier).savedIntent.getStringExtra(EXTRA_MODE))
    }
}
