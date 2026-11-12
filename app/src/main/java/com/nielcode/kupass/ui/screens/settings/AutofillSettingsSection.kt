package com.nielcode.kupass.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.nielcode.kupass.ui.icons.KupassIcons

/** Autofill service status (picker or turn off), the save offer, and Chrome's switch. */
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
        AutofillServiceItem(
            enabled = serviceEnabled,
            onOpenPicker = {
                // The launching activity's bounded allowance covers the system picker.
                onAllowBackground()
                openAutofillPicker(context)
            },
            onDisable = {
                AutofillSettings.disable(context)
                // The system applies it asynchronously, so a re-read now still says "on". The
                // next resume reads the real state again.
                serviceEnabled = false
            },
        )
        SectionItem(
            icon = KupassIcons.Save,
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

/**
 * The autofill service row. While Kupass is the service, a tap offers to turn autofill off: on
 * Android 13 and lower the system picker closes at once for the service that is already selected.
 */
@Composable
internal fun AutofillServiceItem(
    enabled: Boolean,
    onOpenPicker: () -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmOff by rememberSaveable { mutableStateOf(false) }
    SectionItem(
        icon = KupassIcons.Key,
        title = stringResource(R.string.settings_autofill_service),
        subtitle =
            stringResource(
                if (enabled) R.string.settings_autofill_on else R.string.settings_autofill_off
            ),
        onClick = { if (enabled) confirmOff = true else onOpenPicker() },
        modifier = modifier,
    )
    if (confirmOff) {
        AlertDialog(
            onDismissRequest = { confirmOff = false },
            title = { Text(stringResource(R.string.settings_autofill_disable_title)) },
            text = { Text(stringResource(R.string.settings_autofill_disable_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmOff = false
                        onDisable()
                    }
                ) {
                    Text(stringResource(R.string.settings_autofill_disable_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOff = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
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
