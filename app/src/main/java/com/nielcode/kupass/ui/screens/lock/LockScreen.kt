package com.nielcode.kupass.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

/**
 * Shown only instead of a protected destination. No entry data is composed here.
 *
 * @param deviceSecure whether the device has a screen lock. Without one the vault can't be
 *   protected, so the user is guided to set one up.
 */
@Composable
fun LockScreen(
    deviceSecure: Boolean,
    onUnlock: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnUnlock by rememberUpdatedState(onUnlock)
    // Prompt automatically once each time the lock screen appears. This stays a LaunchedEffect on
    // purpose: BiometricPrompt commits a fragment, which must not run in the composition apply
    // phase
    // that a keyed SideEffect would use.
    @Suppress("UnnecessaryLaunchedEffect")
    LaunchedEffect(deviceSecure) { if (deviceSecure) currentOnUnlock() }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text =
                    stringResource(
                        if (deviceSecure) R.string.lock_title else R.string.lock_no_credential_title
                    ),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    stringResource(
                        if (deviceSecure) R.string.lock_message
                        else R.string.lock_no_credential_message
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (deviceSecure) {
                Button(onClick = onUnlock) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.lock_button_unlock))
                }
            } else {
                Button(onClick = onOpenSecuritySettings) {
                    Text(stringResource(R.string.lock_open_settings))
                }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.button_cancel)) }
        }
    }
}

/**
 * Public pages remain usable without credentials; guidance is shown only for a sensitive action.
 */
@Composable
fun SensitiveActionGuidance(
    visible: Boolean,
    onOpenSecuritySettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.lock_no_credential_title)) },
            text = { Text(stringResource(R.string.lock_no_credential_message)) },
            confirmButton = {
                TextButton(onClick = onOpenSecuritySettings) {
                    Text(stringResource(R.string.lock_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
            },
        )
    }
}
