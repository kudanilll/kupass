package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.ui.screens.home.components.VaultList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    // Collect state from ViewModel
    val passwords by viewModel.passwords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Local UI state
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            VaultList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = innerPadding,
                passwords = passwords,
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                onItemClick = { password: PasswordEntity ->
                    onNavigateToDetail(password.id)
                },
                onDeleteItem = { password: PasswordEntity ->
                    viewModel.deletePassword(password)
                }
            )
        }
    }
}
