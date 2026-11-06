package com.nielcode.kupass.autofill

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutofillSettingsTest {

    @Test
    fun `enable intent asks the system to pick Kupass`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = AutofillSettings.enableIntent(context)

        assertEquals(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
    }
}
