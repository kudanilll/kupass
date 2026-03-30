package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LazyColumn(modifier = modifier.fillMaxSize()) {
//        item {
//            if (!active) {
//                Text(
//                    text = stringResource(R.string.headline),
//                    style = MaterialTheme.typography.displayMedium,
//                    color = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.fillMaxWidth(0.8f).padding(start = 16.dp, top = 32.dp),
//                )
//            }
//        }

        stickyHeader {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = if (active) 0.dp else 16.dp)
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { onActiveChange(false) },
                    active = active,
                    onActiveChange = onActiveChange,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (active) {
                            IconButton(
                                onClick = {
                                    if (query.isNotEmpty()) {
                                        onQueryChange("")
                                    } else {
                                        onActiveChange(false)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight),
                ) {
                    if (query.isNotEmpty()) {
                        Text("Mencari: $query", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        if (isEmpty) {
            item { Text("Vault is empty", modifier = Modifier.padding(16.dp)) }
        } else {
            items(20) { index -> Text("Password Item $index", modifier = Modifier.padding(16.dp)) }
        }
    }
}
