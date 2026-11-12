package com.nielcode.kupass.autofill

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.core.net.toUri

/** What the app's Settings screen needs to show and change the autofill service. */
object AutofillSettings {

    /** True when Kupass is the device's active autofill service. */
    fun isKupassEnabled(context: Context): Boolean =
        context.getSystemService(AutofillManager::class.java)?.hasEnabledAutofillServices() == true

    /** Turns autofill off for the user. Works only while Kupass is the active service. */
    fun disable(context: Context) {
        context.getSystemService(AutofillManager::class.java)?.disableAutofillServices()
    }

    /** Opens the system picker with Kupass preselected. */
    fun enableIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            .setData("package:${context.packageName}".toUri())
}
