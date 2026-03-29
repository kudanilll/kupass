package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        if (!active) {
            Text(
                text = stringResource(R.string.headline),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 8.dp),
            )
        }

        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { onActiveChange(false) },
            active = active,
            onActiveChange = onActiveChange,
            placeholder = { Text("Search password...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = if (active) 0.dp else 16.dp)
                    .padding(bottom = if (active) 0.dp else 8.dp),
        ) {
            // Isi dari SearchView (Daftar hasil pencarian) akan dirender di sini saat active = true
            // Panggil komponen daftar password di sini untuk hasil search
        }
    }
}
