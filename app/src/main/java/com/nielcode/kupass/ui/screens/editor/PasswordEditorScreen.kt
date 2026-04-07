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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.ui.components.TextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordEditorScreen(onNavigateBack: () -> Unit) {
    // State form
    var siteName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // State hide/show password
    var passwordVisible by remember { mutableStateOf(false) }

    val isFormValid = siteName.isNotBlank() && password.isNotBlank()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Tambah Sandi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = { /* TODO: Save logic */
                            onNavigateBack()
                        },
                        enabled = isFormValid,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Simpan")
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
                label = "Nama Situs / Aplikasi",
                icon = Icons.Default.Language,
                placeholder = "Cth: Netflix, Google"
            )

            TextField(
                value = username,
                onValueChange = { username = it },
                label = "Username / Email",
                icon = Icons.Default.Person,
                keyboardType = KeyboardType.Email
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                label = "Kata Sandi",
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
                        Icon(imageVector = image, contentDescription = "Toggle Password")
                    }
                }
            )

            TextField(
                value = url,
                onValueChange = { url = it },
                label = "URL Website",
                icon = Icons.Default.Link,
                placeholder = "https://...",
                keyboardType = KeyboardType.Uri
            )

            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Catatan Tambahan",
                icon = Icons.AutoMirrored.Filled.Notes,
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
