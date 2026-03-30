package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.screens.home.components.BottomNav
import com.nielcode.kupass.ui.screens.home.components.FAB
import com.nielcode.kupass.ui.screens.home.components.VaultList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = {
                Text(
                    text = stringResource(R.string.headline),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight()
                )
            })
        },
        floatingActionButton = { FAB(onClick = { /* Navigate to CreatePassword */ }) },
        bottomBar = {
            BottomNav(
                currentRoute = "home",
                onNavigate = { /* Handle navigate tab */ })
        },
    ) { innerPadding ->
        VaultList(
            modifier = Modifier.padding(innerPadding),
            isEmpty = false,
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            active = isSearchActive,
            onActiveChange = { isSearchActive = it },
        )
    }
}
