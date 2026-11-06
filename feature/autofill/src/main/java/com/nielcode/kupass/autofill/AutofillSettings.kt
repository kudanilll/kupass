package com.nielcode.kupass.autofill

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.autofill.AutofillManager

/** What the app's Settings screen needs to show and change the autofill service. */
object AutofillSettings {

    /** True when Kupass is the device's active autofill service. */
    fun isKupassEnabled(context: Context): Boolean =
        context.getSystemService(AutofillManager::class.java)?.hasEnabledAutofillServices() == true

    /** Opens the system picker with Kupass preselected. */
    fun enableIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            .setData(Uri.parse("package:${context.packageName}"))
}
