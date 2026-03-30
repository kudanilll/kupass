package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultList(
    modifier: Modifier = Modifier,
    isEmpty: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            if (!active) {
                Text(
                    text = stringResource(R.string.headline),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(start = 16.dp, top = 32.dp, bottom = 16.dp)
                )
            }
        }

        stickyHeader {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp)
            ) {
                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSearch = { onActiveChange(false) },
                            expanded = active,
                            onExpandedChange = onActiveChange,
                            enabled = true,
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Search",
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clickable {
                                                if (query.isNotEmpty()) {
                                                    onQueryChange("")
                                                } else {
                                                    onActiveChange(false)
                                                }
                                            }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    expanded = active,
                    onExpandedChange = onActiveChange,
                    shape = SearchBarDefaults.dockedShape,
                    colors = SearchBarDefaults.colors(),
                    tonalElevation = SearchBarDefaults.TonalElevation,
                    shadowElevation = SearchBarDefaults.ShadowElevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    content = {
                        if (active) {
                            if (query.isNotEmpty()) {
                                Text("Mencari: $query", modifier = Modifier.padding(16.dp))
                            }
                        }
                    },
                )
            }
        }

        if (isEmpty) {
            item { Text("Vault is empty", modifier = Modifier.padding(16.dp)) }
        } else {
            items(20) { index -> Text("Password Item $index", modifier = Modifier.padding(16.dp)) }
        }
    }
}
