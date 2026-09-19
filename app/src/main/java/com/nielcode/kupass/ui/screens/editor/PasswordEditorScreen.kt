package com.nielcode.kupass.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.ui.components.VaultTextField

private const val URL_PLACEHOLDER = "https://..."

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
    viewModel: PasswordEditorViewModel = viewModel(factory = PasswordEditorViewModel.Factory),
) {
    val isEditMode = passwordId > 0
    val context = LocalContext.current
    val form = remember { EditorFormState() }
    val existingPassword by viewModel.existingPassword.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val saveFailedText = stringResource(R.string.toast_failed_save)

    SideEffect(passwordId) { if (isEditMode) viewModel.loadPassword(passwordId) }
    SideEffect(existingPassword) { existingPassword?.let(form::prefillOnce) }
    SideEffect(saveState) {
        when (saveState) {
            SaveState.Success -> {
                viewModel.resetSaveState()
                currentOnNavigateBack()
            }
            SaveState.Error -> {
                // Keep the form so nothing typed is lost; the user can retry.
                Toast.makeText(context, saveFailedText, Toast.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            EditorTopBar(
                isEditMode = isEditMode,
                saveEnabled = form.isValid && saveState !is SaveState.Saving,
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
        EditorForm(form = form, modifier = Modifier.padding(innerPadding))
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
        get() = siteName.isNotBlank() && password.isNotBlank()

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            FilledIconButton(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.button_save))
            }
        },
    )
}

@Composable
private fun EditorForm(form: EditorFormState, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VaultTextField(
            value = form.siteName,
            onValueChange = { form.siteName = it },
            label = stringResource(R.string.site_or_app_hint),
            icon = Icons.Default.Language,
            placeholder = stringResource(R.string.site_placeholder),
        )
        VaultTextField(
            value = form.username,
            onValueChange = { form.username = it },
            label = stringResource(R.string.username_hint),
            icon = Icons.Default.Person,
            keyboardType = KeyboardType.Email,
        )
        VaultTextField(
            value = form.password,
            onValueChange = { form.password = it },
            label = stringResource(R.string.password_hint),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation =
                if (form.passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { form.passwordVisible = !form.passwordVisible }) {
                    Icon(
                        imageVector =
                            if (form.passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(R.string.show_password),
                    )
                }
            },
        )
        VaultTextField(
            value = form.url,
            onValueChange = { form.url = it },
            label = stringResource(R.string.url_label),
            icon = Icons.Default.Link,
            placeholder = URL_PLACEHOLDER,
            keyboardType = KeyboardType.Uri,
        )
        VaultTextField(
            value = form.notes,
            onValueChange = { form.notes = it },
            label = stringResource(R.string.note_hint),
            icon = Icons.AutoMirrored.Filled.Notes,
            singleLine = false,
        )
    }
}
