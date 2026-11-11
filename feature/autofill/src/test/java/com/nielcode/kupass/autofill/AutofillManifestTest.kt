package com.nielcode.kupass.autofill

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutofillManifestTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun info(activity: Class<*>): ActivityInfo =
        context.packageManager.getActivityInfo(ComponentName(context, activity), 0)

    @Test
    fun `unlock activity outlives the device-credential screen`() {
        // On API 27-29 the PIN screen is another activity; noHistory finishes this one first.
        assertEquals(
            0,
            info(AutofillUnlockActivity::class.java).flags and ActivityInfo.FLAG_NO_HISTORY,
        )
    }
}
