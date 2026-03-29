package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nielcode.kupass.ui.screens.home.components.BottomNav
import com.nielcode.kupass.ui.screens.home.components.FAB
import com.nielcode.kupass.ui.screens.home.components.TopBar
import com.nielcode.kupass.ui.screens.home.components.VaultList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Scroll behavior
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = { FAB(onClick = { /* Navigate to CreatePassword */ }) },
        bottomBar = {
            BottomNav(
                currentRoute = "home", // Dummy state
                onNavigate = { /* Handle navigate tab */ },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            VaultList(modifier = Modifier.padding(innerPadding), isEmpty = false)
        }
    }
}
