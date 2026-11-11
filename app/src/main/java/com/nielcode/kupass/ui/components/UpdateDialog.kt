package com.nielcode.kupass.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.nielcode.kupass.R
import com.nielcode.kupass.update.UpdateState

private const val PERCENT = 100f

/**
 * The in-app update dialog for every [UpdateState] except Idle.
 *
 * @param onDownload "Download", and "Try again" after a failure.
 * @param onClose back or a tap outside: hidden until the app opens again.
 * @param onOpenPage the release page in the browser, the fallback when updating fails.
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UpdateState.Idle -> Unit
        is UpdateState.Available ->
            UpdateAlert(
                title = stringResource(R.string.update_title),
                text = stringResource(R.string.update_available, state.update.version.toString()),
                onDismissRequest = onClose,
                confirmLabel = stringResource(R.string.update_download),
                onConfirm = onDownload,
                dismissLabel = stringResource(R.string.update_later),
                onDismiss = onLater,
                modifier = modifier,
            )
        is UpdateState.Downloading ->
            UpdateAlert(
                title = stringResource(R.string.update_title),
                text = stringResource(R.string.update_downloading, state.update.version.toString()),
                // Back cancels; a stray tap outside doesn't.
                onDismissRequest = onCancel,
                confirmLabel = stringResource(R.string.button_cancel),
                onConfirm = onCancel,
                modifier = modifier,
                progress = state.percent / PERCENT,
            )
        is UpdateState.NeedsPermission ->
            UpdateAlert(
                title = stringResource(R.string.update_title),
                text = stringResource(R.string.update_permission),
                onDismissRequest = onClose,
                confirmLabel = stringResource(R.string.update_open_settings),
                onConfirm = onOpenSettings,
                dismissLabel = stringResource(R.string.button_cancel),
                onDismiss = onClose,
                modifier = modifier,
            )
        is UpdateState.Installing ->
            UpdateAlert(
                title = stringResource(R.string.update_title),
                text = stringResource(R.string.update_installing, state.update.version.toString()),
                onDismissRequest = onClose,
                modifier = modifier,
                indeterminate = true,
            )
        is UpdateState.Failed ->
            UpdateAlert(
                title = stringResource(R.string.update_failed_title),
                text = stringResource(R.string.update_failed, state.update.version.toString()),
                onDismissRequest = onClose,
                confirmLabel = stringResource(R.string.update_retry),
                onConfirm = onDownload,
                dismissLabel = stringResource(R.string.update_open_browser),
                onDismiss = { onOpenPage(state.update.pageUrl) },
                modifier = modifier,
            )
    }
}

@Composable
private fun UpdateAlert(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {},
    progress: Float? = null,
    indeterminate: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = progress == null),
        title = { Text(title) },
        text = {
            Column {
                Text(text)
                val indicator = Modifier.fillMaxWidth().padding(top = 16.dp)
                when {
                    progress != null -> LinearProgressIndicator({ progress }, indicator)
                    indeterminate -> LinearProgressIndicator(indicator)
                }
            }
        },
        confirmButton = {
            confirmLabel?.let { TextButton(onClick = onConfirm) { Text(it) } }
        },
        dismissButton = dismissLabel?.let { { TextButton(onClick = onDismiss) { Text(it) } } },
    )
}
