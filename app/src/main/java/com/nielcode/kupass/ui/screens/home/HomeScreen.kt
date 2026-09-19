package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.ui.screens.home.components.VaultList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    // Collect state from ViewModel
    val passwords by viewModel.passwords.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Local UI state
    var isSearchActive by remember { mutableStateOf(false) }
    var passwordToDelete by remember { mutableStateOf<PasswordEntity?>(null) }

    // Dialog confirmation for swipe-to-delete to prevent accidental data loss.
    if (passwordToDelete != null) {
        AlertDialog(
            onDismissRequest = { passwordToDelete = null },
            title = { Text(stringResource(R.string.dialog_title_delete)) },
            text = {
                Text(
                    "${stringResource(R.string.dialog_message_delete)} \"${passwordToDelete?.siteName}\"?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        passwordToDelete?.let { viewModel.deletePassword(it) }
                        passwordToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.button_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordToDelete = null }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            VaultList(
                modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
                contentPadding = innerPadding,
                passwords = passwords,
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                onItemClick = { password: PasswordEntity -> onNavigateToDetail(password.id) },
                onDeleteItem = { password: PasswordEntity -> passwordToDelete = password },
            )
        }
    }
}
