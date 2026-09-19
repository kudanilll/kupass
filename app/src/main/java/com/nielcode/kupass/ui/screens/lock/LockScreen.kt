package com.nielcode.kupass.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

/**
 * Shown instead of the whole app while the vault is locked. No vault data is composed here.
 *
 * @param deviceSecure whether the device has a screen lock. Without one the vault can't be
 *   protected, so the user is guided to set one up.
 */
@Composable
fun LockScreen(deviceSecure: Boolean, onUnlock: () -> Unit, onOpenSecuritySettings: () -> Unit) {
    // Prompt automatically once each time the lock screen appears.
    LaunchedEffect(deviceSecure) { if (deviceSecure) onUnlock() }

    Scaffold { innerPadding ->
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
                text = stringResource(if (deviceSecure) R.string.lock_title else R.string.lock_no_credential_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(if (deviceSecure) R.string.lock_message else R.string.lock_no_credential_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (deviceSecure) {
                Button(onClick = onUnlock) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.lock_button_unlock))
                }
            } else {
                Button(onClick = onOpenSecuritySettings) { Text(stringResource(R.string.lock_open_settings)) }
            }
        }
    }
}
