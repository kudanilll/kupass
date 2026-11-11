package com.nielcode.kupass.ui.components

import androidx.annotation.StringRes
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

/** What a button of the update dialog does. */
internal enum class UpdateAction {
    Download,
    Close,
    Cancel,
    OpenSettings,
    OpenPage,
}

internal data class UpdateButton(@param:StringRes val label: Int, val action: UpdateAction)

/** What the update dialog shows for a state; plain data, so it is tested on the JVM. */
internal data class UpdateDialogContent(
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
    /** [text] takes the version, as in "Kupass 3.1.0-beta.2 is ready to install". */
    val namesVersion: Boolean = true,
    val confirm: UpdateButton? = null,
    val dismiss: UpdateButton? = null,
    /** Back, or a tap outside while nothing is in progress. */
    val onDismissRequest: UpdateAction = UpdateAction.Close,
    /** Download progress from 0 to 1; a tap outside doesn't close the dialog then. */
    val progress: Float? = null,
    /** An indeterminate progress bar. */
    val busy: Boolean = false,
)

/** The dialog for [state]; null while idle. */
internal fun updateDialogContent(state: UpdateState): UpdateDialogContent? =
    when (state) {
        UpdateState.Idle -> null
        is UpdateState.Available ->
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_available,
                confirm = UpdateButton(R.string.update_download, UpdateAction.Download),
                dismiss = UpdateButton(R.string.update_later, UpdateAction.Close),
            )
        is UpdateState.Downloading ->
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_downloading,
                confirm = UpdateButton(R.string.button_cancel, UpdateAction.Cancel),
                onDismissRequest = UpdateAction.Cancel,
                progress = state.percent / PERCENT,
            )
        is UpdateState.NeedsPermission ->
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_permission,
                namesVersion = false,
                confirm = UpdateButton(R.string.update_open_settings, UpdateAction.OpenSettings),
                dismiss = UpdateButton(R.string.button_cancel, UpdateAction.Close),
            )
        is UpdateState.Installing ->
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_installing,
                busy = true,
            )
        is UpdateState.Failed ->
            UpdateDialogContent(
                title = R.string.update_failed_title,
                text = R.string.update_failed,
                confirm = UpdateButton(R.string.update_retry, UpdateAction.Download),
                dismiss = UpdateButton(R.string.update_open_browser, UpdateAction.OpenPage),
            )
    }

/**
 * The in-app update dialog for every [UpdateState] except Idle.
 *
 * @param onDownload "Download", and "Try again" after a failure.
 * @param onClose "Later", back, or a tap outside: hidden until the app opens again.
 * @param onOpenPage the release page in the browser, the fallback when updating fails.
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = updateDialogContent(state) ?: return
    val update = state.update ?: return
    val run: (UpdateAction) -> Unit = { action ->
        when (action) {
            UpdateAction.Download -> onDownload()
            UpdateAction.Close -> onClose()
            UpdateAction.Cancel -> onCancel()
            UpdateAction.OpenSettings -> onOpenSettings()
            UpdateAction.OpenPage -> onOpenPage(update.pageUrl)
        }
    }
    val text =
        if (content.namesVersion) stringResource(content.text, update.version.toString())
        else stringResource(content.text)
    AlertDialog(
        onDismissRequest = { run(content.onDismissRequest) },
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = content.progress == null),
        title = { Text(stringResource(content.title)) },
        text = {
            Column {
                Text(text)
                val indicator = Modifier.fillMaxWidth().padding(top = 16.dp)
                when {
                    content.progress != null ->
                        LinearProgressIndicator({ content.progress }, indicator)
                    content.busy -> LinearProgressIndicator(indicator)
                }
            }
        },
        confirmButton = { content.confirm?.let { UpdateTextButton(it, run) } },
        dismissButton = content.dismiss?.let { { UpdateTextButton(it, run) } },
    )
}

@Composable
private fun UpdateTextButton(button: UpdateButton, run: (UpdateAction) -> Unit) {
    TextButton(onClick = { run(button.action) }) { Text(stringResource(button.label)) }
}
