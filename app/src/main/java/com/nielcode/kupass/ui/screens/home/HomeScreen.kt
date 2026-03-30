package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nielcode.kupass.ui.screens.home.components.VaultList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            VaultList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = innerPadding,
                isEmpty = false,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
            )
        }
    }
}
