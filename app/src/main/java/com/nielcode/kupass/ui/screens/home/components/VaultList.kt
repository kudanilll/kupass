package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.filterByQuery
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.data.siteicon.siteDomainOf
import com.nielcode.kupass.ui.components.EmptyState
import com.nielcode.kupass.ui.screens.home.VaultGroup
import com.nielcode.kupass.ui.screens.home.groupVault

private const val EMPTY_STATE_HEIGHT_FRACTION = 0.7f

/**
 * Scrollable vault: headline, a single sticky search input, groups and per-account deletion.
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    siteIcons: SiteIcons? = null,
) {
    val groups = remember(passwords, query) { groupVault(passwords.filterByQuery(query)) }
    var expandedKeys by remember { mutableStateOf(emptyList<String>()) }
    val searching = query.isNotBlank()
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            item(key = "headline", contentType = "headline") {
                if (!searchActive) {
                    Text(
                        text = stringResource(R.string.headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(0.8f).padding(start = 16.dp, top = 32.dp),
                    )
                }
            }
            stickyHeader(key = "search", contentType = "search") {
                VaultSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    active = searchActive,
                    onActiveChange = onSearchActiveChange,
                )
            }
            if (groups.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    VaultEmptyState(
                        query = query,
                        modifier = Modifier.fillParentMaxHeight(EMPTY_STATE_HEIGHT_FRACTION),
                    )
                }
            } else {
                groups.forEach { group ->
                    val key = group.key.lazyKey
                    vaultGroup(
                        group = group,
                        expanded = searching || key in expandedKeys,
                        onToggle =
                            if (searching) null
                            else {
                                {
                                    expandedKeys =
                                        if (key in expandedKeys) expandedKeys - key
                                        else expandedKeys + key
                                }
                            },
                        onItemClick = onItemClick,
                        onDeleteItem = onDeleteItem,
                        siteIcons = siteIcons,
                    )
                }
            }
        }
        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// A lazy-list builder carries composable state/callbacks but cannot itself be @Composable.
@Suppress("LongParameterList")
private fun LazyListScope.vaultGroup(
    group: VaultGroup,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
    onItemClick: (PasswordEntity) -> Unit,
    onDeleteItem: (PasswordEntity) -> Unit,
    siteIcons: SiteIcons?,
) {
    item(key = group.key.lazyKey, contentType = "group") {
        VaultGroupHeader(group = group, expanded = expanded, onToggle = onToggle)
    }
    if (expanded) {
        items(items = group.accounts, key = { "account:${it.id}" }, contentType = { "entry" }) {
            password ->
            SwipeToDeleteRow(
                password = password,
                siteIcons = siteIcons,
                onClick = { onItemClick(password) },
                onDelete = { onDeleteItem(password) },
            )
        }
    }
}

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
        VaultSearchInput(query, onQueryChange, active, onActiveChange)
    }
}

@Composable
private fun VaultSearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val closeSearch: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = CircleShape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (active || query.isNotEmpty()) {
                VaultSearchAction(
                    query = query,
                    onQueryChange = onQueryChange,
                    onClose = closeSearch,
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { closeSearch() }),
        modifier =
            modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp).onFocusChanged {
                onActiveChange(it.isFocused)
            },
    )
}

@Composable
private fun VaultSearchAction(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { if (query.isNotEmpty()) onQueryChange("") else onClose() },
        modifier = modifier,
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription =
                stringResource(
                    if (query.isNotEmpty()) R.string.search_clear else R.string.search_close
                ),
        )
    }
}

/** Even a singleton has a header; headers never navigate or delete accounts. */
@Composable
private fun VaultGroupHeader(
    group: VaultGroup,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val action =
        stringResource(if (expanded) R.string.vault_group_collapse else R.string.vault_group_expand)
    val state =
        stringResource(
            if (expanded) R.string.vault_group_expanded else R.string.vault_group_collapsed
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .semantics { stateDescription = state }
                .clickable(
                    enabled = onToggle != null,
                    role = Role.Button,
                    onClickLabel = action,
                    onClick = { onToggle?.invoke() },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.key.value.ifBlank { stringResource(R.string.vault_group_unnamed) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.vault_group_accounts,
                        group.accounts.size,
                        group.accounts.size,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
        )
    }
}

/** Empty vault or a search without results. */
@Composable
private fun VaultEmptyState(query: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (query.isBlank()) {
            EmptyState(
                icon = Icons.Default.Key,
                title = stringResource(R.string.empty_vault_title),
                body = stringResource(R.string.empty_vault_body),
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

/** A vault row. A rightward swipe asks for confirmation (via [onDelete]) and resets. */
@Composable
private fun SwipeToDeleteRow(
    password: PasswordEntity,
    siteIcons: SiteIcons?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offset by remember(password.id) { mutableFloatStateOf(0f) }
    val delete by rememberUpdatedState(onDelete)
    val deleteLabel = stringResource(R.string.button_delete)
    Box(
        modifier =
            modifier
                .clipToBounds()
                .rightwardDeleteGesture(
                    password.id,
                    onOffsetChange = { offset = it },
                    onDelete = { delete() },
                )
    ) {
        if (offset > 0f) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .padding(vertical = 2.dp)
                        .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = AbsoluteAlignment.CenterLeft,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        PasswordListItem(
            title = password.siteName,
            subtitle = password.username.ifBlank { password.url },
            fallbackChar = password.siteName.firstOrNull()?.uppercase() ?: "?",
            onClick = onClick,
            icon = rememberSiteIcon(password, siteIcons),
            modifier =
                Modifier.graphicsLayer { translationX = offset }
                    .semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(deleteLabel) {
                                    delete()
                                    true
                                }
                            )
                    },
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
