package com.nielcode.kupass.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.components.TextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordEditorScreen(
    passwordId: Long = -1L,
    onNavigateBack: () -> Unit,
    viewModel: PasswordEditorViewModel = viewModel()
) {
    val isEditMode = passwordId > 0

    // State form
    var siteName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // State hide/show password
    var passwordVisible by remember { mutableStateOf(false) }

    // Track if form has been pre-filled (to avoid overwriting user edits)
    var hasPreFilled by remember { mutableStateOf(false) }

    // Load existing password in edit mode
    LaunchedEffect(passwordId) {
        if (isEditMode) {
            viewModel.loadPassword(passwordId)
        }
    }

    // Pre-fill form when existing password is loaded
    val existingPassword by viewModel.existingPassword.collectAsState()
    LaunchedEffect(existingPassword) {
        val existing = existingPassword
        if (existing != null && !hasPreFilled) {
            siteName = existing.siteName
            username = existing.username
            password = existing.password
            url = existing.url
            notes = existing.notes
            hasPreFilled = true
        }
    }

    val isFormValid = siteName.isNotBlank() && password.isNotBlank()

    // Observe save state
    val saveState by viewModel.saveState.collectAsState()

    // Navigate back on successful save
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    val isSaving = saveState is SaveState.Saving

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (isEditMode) R.string.edit_password
                            else R.string.create_new_password
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = {
                            viewModel.savePassword(
                                siteName = siteName,
                                username = username,
                                password = password,
                                url = url,
                                notes = notes
                            )
                        },
                        enabled = isFormValid && !isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.button_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = siteName,
                onValueChange = { siteName = it },
                label = stringResource(R.string.site_or_app_hint),
                icon = Icons.Default.Language,
                placeholder = "Cth: Netflix, Google"
            )

            TextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(R.string.username_hint),
                icon = Icons.Default.Person,
                keyboardType = KeyboardType.Email
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.password_hint),
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    val image =
                        if (passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = stringResource(R.string.show_password))
                    }
                }
            )

            TextField(
                value = url,
                onValueChange = { url = it },
                label = stringResource(R.string.url_label),
                icon = Icons.Default.Link,
                placeholder = "https://...",
                keyboardType = KeyboardType.Uri
            )

            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = stringResource(R.string.note_hint),
                icon = Icons.AutoMirrored.Filled.Notes,
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
