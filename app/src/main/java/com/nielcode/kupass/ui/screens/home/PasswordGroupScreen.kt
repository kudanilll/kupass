package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.ui.components.DeletePasswordDialog
import com.nielcode.kupass.ui.screens.VaultEventToasts
import com.nielcode.kupass.ui.screens.home.components.VaultList

/** A public account list; opening any account still goes through the entry authentication gate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGroupScreen(
    groupId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    siteIcons: SiteIcons? = null,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val passwords by viewModel.allPasswords.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    // Only a row ID is saved. Site names, URLs and credentials never enter saved state.
    var anchorId by rememberSaveable(groupId) { mutableLongStateOf(groupId) }
    var groupKey by remember(groupId) { mutableStateOf<VaultGroupKey?>(null) }
    val resolvedKey = groupKey ?: passwords.firstOrNull { it.id == anchorId }?.let(::vaultGroupKey)
    val accounts =
        remember(passwords, resolvedKey) {
            passwords.filter { resolvedKey != null && vaultGroupKey(it) == resolvedKey }
        }
    SideEffect(resolvedKey, accounts) {
        if (groupKey == null) groupKey = resolvedKey
        if (accounts.isNotEmpty() && accounts.none { it.id == anchorId }) {
            anchorId = accounts.first().id
        }
    }
    var searchActive by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PasswordEntity?>(null) }
    pendingDelete?.let { account ->
        DeletePasswordDialog(
            siteName = account.siteName,
            onConfirm = {
                viewModel.deletePassword(account)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
    VaultEventToasts(viewModel.events)
    Scaffold(
        modifier = modifier,
        topBar = { GroupTopBar(resolvedKey?.value, onNavigateBack) },
    ) { padding ->
        VaultList(
            passwords = accounts,
            query = query,
            onQueryChange = viewModel::onSearchQueryChange,
            searchActive = searchActive,
            onSearchActiveChange = { searchActive = it },
            onItemClick = { onNavigateToDetail(it.id) },
            onDeleteItem = { pendingDelete = it },
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            siteIcons = siteIcons,
            showHeadline = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupTopBar(title: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                title ?: stringResource(R.string.vault_group_unnamed),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back))
            }
        },
    )
}
