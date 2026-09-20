package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.data.siteicon.siteDomainOf
import com.nielcode.kupass.ui.components.EmptyState
import kotlinx.coroutines.launch

private const val EMPTY_STATE_HEIGHT_FRACTION = 0.7f

/**
 * Scrollable vault: headline, sticky search bar, and swipe-to-delete rows.
 *
 * @param siteIcons icon loader, or null when site icons are turned off.
 */
@Composable
fun VaultList(
    passwords: List<PasswordEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onItemClick: (PasswordEntity) -> Unit,
    onDeleteItem: (PasswordEntity) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    siteIcons: SiteIcons? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            item(contentType = "headline") {
                if (!searchActive) {
                    Text(
                        text = stringResource(R.string.headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(0.8f).padding(start = 16.dp, top = 32.dp),
                    )
                }
            }
            stickyHeader(contentType = "search") {
                VaultSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    active = searchActive,
                    onActiveChange = onSearchActiveChange,
                )
            }
            if (passwords.isEmpty()) {
                item(contentType = "empty") {
                    VaultEmptyState(
                        query = query,
                        onAddClick = onAddClick,
                        modifier = Modifier.fillParentMaxHeight(EMPTY_STATE_HEIGHT_FRACTION),
                    )
                }
            } else {
                items(items = passwords, key = { it.id }, contentType = { "entry" }) { password ->
                    SwipeToDeleteRow(
                        password = password,
                        siteIcons = siteIcons,
                        onClick = { onItemClick(password) },
                        onDelete = { onDeleteItem(password) },
                    )
                }
            }
        }
        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(surface, surface.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
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
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (active) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_close),
                                modifier =
                                    Modifier.padding(8.dp).clickable {
                                        // First tap clears the query, second tap closes search.
                                        if (query.isNotEmpty()) onQueryChange("")
                                        else onActiveChange(false)
                                    },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            expanded = active,
            onExpandedChange = onActiveChange,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        ) {
            if (active && query.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_searching_for, query),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** Empty vault (with an "add" action) or a search without results. */
@Composable
private fun VaultEmptyState(query: String, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (query.isBlank()) {
            EmptyState(
                icon = Icons.Default.Key,
                title = stringResource(R.string.empty_vault_title),
                body = stringResource(R.string.empty_vault_body),
                actionLabel = stringResource(R.string.empty_vault_action),
                onAction = onAddClick,
            )
        } else {
            EmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.empty_search_title),
                body = stringResource(R.string.empty_search_body, query.trim()),
            )
        }
    }
}

/** A vault row. Swiping it asks for confirmation (via [onDelete]) and slides back. */
@Composable
private fun SwipeToDeleteRow(
    password: PasswordEntity,
    siteIcons: SiteIcons?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val coroutineScope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(vertical = 2.dp)
                        .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.button_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                // Deletion happens only from the confirmation dialog; the row slides back.
                onDelete()
                coroutineScope.launch { dismissState.reset() }
            }
        },
    ) {
        PasswordListItem(
            title = password.siteName,
            subtitle = password.username.ifBlank { password.url },
            fallbackChar = password.siteName.firstOrNull()?.uppercase() ?: "?",
            onClick = onClick,
            icon = rememberSiteIcon(password, siteIcons),
        )
    }
}

/** The row's site icon: from memory right away, otherwise loaded in the background. */
@Composable
private fun rememberSiteIcon(password: PasswordEntity, siteIcons: SiteIcons?): ImageBitmap? {
    val domain =
        remember(password.url, password.siteName) { siteDomainOf(password.url, password.siteName) }
    val icon by
        produceState(domain?.let { siteIcons?.peek(it) }, domain, siteIcons) {
            // produceState keeps the old value when its keys change, so reset it first.
            value = domain?.let { siteIcons?.peek(it) }
            if (value == null && domain != null && siteIcons != null) {
                value = siteIcons.load(domain)
            }
        }
    return icon
}

/** Fades list content out behind the floating bottom navigation. */
@Composable
private fun BottomFade(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to surface.copy(alpha = 0.7f),
                        1f to surface,
                    )
                )
    )
}
