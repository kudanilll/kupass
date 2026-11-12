package com.nielcode.kupass.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.App
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.PasswordGenerator
import com.nielcode.kupass.ui.components.PasswordVisibilityToggle
import com.nielcode.kupass.ui.components.SecretKeyboardOptions
import com.nielcode.kupass.ui.components.VaultFieldShape
import com.nielcode.kupass.ui.components.VaultTextField
import com.nielcode.kupass.ui.components.pageVerticalScroll
import com.nielcode.kupass.ui.icons.KupassIcons

private const val URL_PLACEHOLDER = "https://..."

private val NextFieldKeyboard = KeyboardOptions(imeAction = ImeAction.Next)
private val UsernameKeyboard =
    KeyboardOptions(
        keyboardType = KeyboardType.Email,
        autoCorrectEnabled = false,
        imeAction = ImeAction.Next,
    )
private val UrlKeyboard =
    KeyboardOptions(
        keyboardType = KeyboardType.Uri,
        autoCorrectEnabled = false,
        imeAction = ImeAction.Next,
    )

/**
 * Creates a password entry, or edits one when [passwordId] is positive.
 *
 * The form lives in plain `remember` state on purpose: `rememberSaveable` would put the password
 * into the saved instance state.
 */
@Composable
fun PasswordEditorScreen(
    passwordId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordEditorViewModel =
        viewModel(factory = PasswordEditorViewModel.factory(passwordId)),
) {
    val isEditMode = passwordId > 0
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as? App)?.container }
    val excludeLookalike = container?.preferenceManager?.excludeLookalike ?: false
    val form = remember { EditorFormState() }
    val existingPassword by viewModel.existingPassword.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) { onDispose { viewModel.clearSensitiveState() } }
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val saveFailedText = stringResource(R.string.toast_failed_save)

    SideEffect(passwordId) { if (isEditMode) viewModel.loadPassword(passwordId) }
    SideEffect(existingPassword) { existingPassword?.let(form::prefillOnce) }
    HandleSaveState(
        saveState = saveState,
        saveFailedText = saveFailedText,
        onSuccess = currentOnNavigateBack,
        onReset = viewModel::resetSaveState,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            EditorTopBar(
                isEditMode = isEditMode,
                saveEnabled =
                    form.isValid &&
                        saveState !is SaveState.Saving &&
                        (!isEditMode || existingPassword != null),
                onBack = onNavigateBack,
                onSave = {
                    viewModel.savePassword(
                        siteName = form.siteName,
                        username = form.username,
                        password = form.password,
                        url = form.url,
                        notes = form.notes,
                    )
                },
            )
        },
    ) { innerPadding ->
        if (loadFailed) {
            Text(
                text = stringResource(R.string.editor_load_failed),
                modifier = Modifier.padding(innerPadding).padding(20.dp),
            )
        } else {
            EditorForm(
                form = form,
                excludeLookalike = excludeLookalike,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HandleSaveState(
    saveState: SaveState,
    saveFailedText: String,
    onSuccess: () -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    SideEffect(saveState) {
        when (saveState) {
            SaveState.Success -> {
                onReset()
                onSuccess()
            }

            SaveState.Error -> {
                Toast.makeText(context, saveFailedText, Toast.LENGTH_LONG).show()
                onReset()
            }

            else -> Unit
        }
    }
}

/** Editable fields of the password form. */
@Stable
private class EditorFormState {
    var siteName by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var url by mutableStateOf("")
    var notes by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    private var prefilled = false

    val isValid: Boolean
        get() = siteName.isNotBlank() && password.isNotEmpty()

    /** Copies [entry] into the form once, so later reloads never overwrite the user's edits. */
    fun prefillOnce(entry: PasswordEntity) {
        if (prefilled) return
        siteName = entry.siteName
        username = entry.username
        password = entry.password
        url = entry.url
        notes = entry.notes
        prefilled = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    isEditMode: Boolean,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text =
                    stringResource(
                        if (isEditMode) R.string.edit_password else R.string.create_new_password
                    ),
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                Icon(KupassIcons.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            FilledIconButton(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(KupassIcons.Check, contentDescription = stringResource(R.string.button_save))
            }
        },
    )
}

@Composable
private fun EditorForm(
    form: EditorFormState,
    excludeLookalike: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding() // keep the focused field above the keyboard (before verticalScroll)
                .padding(horizontal = 20.dp)
                .pageVerticalScroll(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VaultTextField(
            value = form.siteName,
            onValueChange = { form.siteName = it },
            label = stringResource(R.string.site_or_app_hint),
            placeholder = stringResource(R.string.site_placeholder),
            keyboardOptions = NextFieldKeyboard,
        )
        VaultTextField(
            value = form.username,
            onValueChange = { form.username = it },
            label = stringResource(R.string.username_hint),
            keyboardOptions = UsernameKeyboard,
        )
        PasswordFieldRow(form = form, excludeLookalike = excludeLookalike)
        VaultTextField(
            value = form.url,
            onValueChange = { form.url = it },
            label = stringResource(R.string.url_label),
            placeholder = URL_PLACEHOLDER,
            keyboardOptions = UrlKeyboard,
        )
        VaultTextField(
            value = form.notes,
            onValueChange = { form.notes = it },
            label = stringResource(R.string.note_hint),
            singleLine = false,
        )
    }
}

@Composable
private fun PasswordFieldRow(
    form: EditorFormState,
    excludeLookalike: Boolean,
    modifier: Modifier = Modifier,
) {
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VaultTextField(
            value = form.password,
            onValueChange = { form.password = it },
            label = stringResource(R.string.password_hint),
            modifier = Modifier.weight(1f),
            keyboardOptions = SecretKeyboardOptions,
            visualTransformation =
                if (form.passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                PasswordVisibilityToggle(
                    visible = form.passwordVisible,
                    onToggle = { form.passwordVisible = !form.passwordVisible },
                )
            },
        )
        FilledTonalIconButton(
            onClick = {
                if (form.password.isBlank()) {
                    form.password = PasswordGenerator.generate(excludeLookalike = excludeLookalike)
                    form.passwordVisible = true
                } else {
                    showReplaceConfirmDialog = true
                }
            },
            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
            shape = VaultFieldShape,
        ) {
            Icon(
                imageVector = KupassIcons.Shuffle,
                contentDescription = stringResource(R.string.generate_password),
            )
        }
    }

    if (showReplaceConfirmDialog) {
        ReplacePasswordConfirmDialog(
            onDismiss = { showReplaceConfirmDialog = false },
            onConfirm = {
                form.password = PasswordGenerator.generate(excludeLookalike = excludeLookalike)
                form.passwordVisible = true
                showReplaceConfirmDialog = false
            },
        )
    }
}

@Composable
private fun ReplacePasswordConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_replace_password_title)) },
        text = { Text(stringResource(R.string.dialog_replace_password_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.button_replace))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
        modifier = modifier,
    )
}
