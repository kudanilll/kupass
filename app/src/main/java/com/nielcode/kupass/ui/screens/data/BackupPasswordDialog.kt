package com.nielcode.kupass.ui.screens.data

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.nielcode.kupass.R
import com.nielcode.kupass.data.backup.BackupCodec
import com.nielcode.kupass.ui.components.PasswordVisibilityToggle
import com.nielcode.kupass.ui.components.SecretKeyboardOptions
import com.nielcode.kupass.ui.components.VaultTextField
import com.nielcode.kupass.ui.icons.KupassIcons

/**
 * Asks for a new backup password before export. [onConfirm] receives a fresh [CharArray]; the
 * receiver owns it and must clear it after use.
 */
@Composable
fun ExportPasswordDialog(onConfirm: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val tooShort = password.length < BackupCodec.MIN_PASSWORD_LENGTH
    val mismatch = confirm.isNotEmpty() && confirm != password

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_export_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_export_dialog_message))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.backup_password_label),
                    error =
                        if (password.isNotEmpty() && tooShort) {
                            pluralStringResource(
                                R.plurals.backup_password_too_short,
                                BackupCodec.MIN_PASSWORD_LENGTH,
                                BackupCodec.MIN_PASSWORD_LENGTH,
                            )
                        } else {
                            null
                        },
                )
                PasswordField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = stringResource(R.string.backup_password_confirm_label),
                    error =
                        if (mismatch) stringResource(R.string.backup_password_mismatch) else null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password.toCharArray()) },
                enabled = !tooShort && confirm == password,
            ) {
                Text(stringResource(R.string.button_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
        },
    )
}

/** Asks for the password of an encrypted backup being imported. */
@Composable
fun ImportPasswordDialog(
    wrongPassword: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_import_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_import_dialog_message))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.backup_password_label),
                    error =
                        if (wrongPassword) stringResource(R.string.backup_wrong_password) else null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password.toCharArray()) },
                enabled = password.isNotEmpty(),
            ) {
                Text(stringResource(R.string.button_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
        },
    )
}

/** Blocking progress while PBKDF2 and AES run. Can't be dismissed, to avoid double submits. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BackupProgressDialog(
    modifier: Modifier = Modifier,
    operation: BackupOperation? = null,
    progress: BackupViewModel.BackupProgress? = null,
) {
    BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier.fillMaxWidth().padding(24.dp).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
            ) {
                ModernLoadingSpinner()
                Text(
                    text =
                        stringResource(
                            when (operation) {
                                BackupOperation.Import -> R.string.backup_import_working
                                BackupOperation.Export -> R.string.backup_export_working
                                null -> R.string.backup_working
                            }
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (progress != null && progress.total > 0) {
                    BackupProgressContent(progress)
                }
            }
        }
    }
}

@Composable
private fun BackupProgressContent(
    progress: BackupViewModel.BackupProgress,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        LinearProgressIndicator(
            progress = {
                (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            drawStopIndicator = {},
        )
        Text(
            text =
                pluralStringResource(
                    R.plurals.backup_progress,
                    progress.total,
                    progress.current,
                    progress.total,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ModernLoadingSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rotation",
        )
    val scale by
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scale",
        )
    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
            strokeWidth = 3.5.dp,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            color = MaterialTheme.colorScheme.primary,
            strokeCap = StrokeCap.Round,
        )
        Icon(
            KupassIcons.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier.size(24.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    VaultTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardOptions = SecretKeyboardOptions,
        visualTransformation =
            if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = error != null,
        supportingText = error,
        trailingIcon = {
            PasswordVisibilityToggle(visible = visible, onToggle = { visible = !visible })
        },
    )
}
