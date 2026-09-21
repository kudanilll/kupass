package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.ui.components.DeletePasswordDialog
import com.nielcode.kupass.ui.screens.home.components.VaultList
import com.nielcode.kupass.ui.screens.pagerPageInsets

/** The vault list with search. Stateless: the caller owns the data and handles every action. */
@Composable
fun HomeScreen(
    passwords: List<PasswordEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeletePassword: (PasswordEntity) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    siteIcons: SiteIcons? = null,
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var passwordToDelete by remember { mutableStateOf<PasswordEntity?>(null) }

    passwordToDelete?.let { pending ->
        DeletePasswordDialog(
            siteName = pending.siteName,
            onConfirm = {
                onDeletePassword(pending)
                passwordToDelete = null
            },
            onDismiss = { passwordToDelete = null },
        )
    }

    Scaffold(modifier = modifier.fillMaxSize(), contentWindowInsets = pagerPageInsets()) {
        innerPadding ->
        VaultList(
            passwords = passwords,
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            searchActive = isSearchActive,
            onSearchActiveChange = { isSearchActive = it },
            onItemClick = { onNavigateToDetail(it.id) },
            onDeleteItem = { passwordToDelete = it },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            contentPadding = contentPadding,
            siteIcons = siteIcons,
        )
    }
}
