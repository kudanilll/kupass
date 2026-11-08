package com.nielcode.kupass.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nielcode.kupass.R
import com.nielcode.kupass.autofill.AutofillSettings
import com.nielcode.kupass.ui.components.SectionHeader
import com.nielcode.kupass.ui.components.SectionItem

/** Autofill service status (opens the system picker), the save offer, and Chrome's switch. */
@Composable
internal fun AutofillSection(
    onAllowBackground: () -> Unit,
    onOfferToSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Re-read on resume: the user may have changed the service in system settings.
    var serviceEnabled by remember { mutableStateOf(AutofillSettings.isKupassEnabled(context)) }
    LifecycleResumeEffect(context) {
        serviceEnabled = AutofillSettings.isKupassEnabled(context)
        onPauseOrDispose {}
    }
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_autofill))
        SectionItem(
            icon = Icons.Default.Key,
            title = stringResource(R.string.settings_autofill_service),
            subtitle =
                stringResource(
                    if (serviceEnabled) R.string.settings_autofill_on
                    else R.string.settings_autofill_off
                ),
            onClick = {
                // The launching activity's bounded allowance covers the system picker.
                onAllowBackground()
                openAutofillPicker(context)
            },
        )
        SectionItem(
            icon = Icons.Default.Save,
            title = stringResource(R.string.settings_offer_save),
            subtitle = stringResource(R.string.settings_offer_save_desc),
            onClick = onOfferToSaveClick,
        )
        Text(
            text = stringResource(R.string.settings_autofill_chrome_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

private fun openAutofillPicker(context: Context) {
    try {
        context.startActivity(AutofillSettings.enableIntent(context))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
