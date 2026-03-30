package com.nielcode.kupass.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.screens.settings.components.SectionHeader
import com.nielcode.kupass.ui.screens.settings.components.SettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.displaySmall,
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->

        val bottomPadding = innerPadding.calculateBottomPadding() + 88.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            // --- Section: General (Language) ---
            item {
                SettingItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.lang_en),
                    onClick = { /* TODO: Buka modal ganti bahasa */ }
                )
            }

            // --- Section: Appearance ---
            item { SectionHeader(title = stringResource(R.string.settings_category_appearance)) }

            item {
                SettingItem(
                    icon = Icons.Default.Contrast,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.theme_light),
                    onClick = { /* TODO: Ganti tema */ }
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.ColorLens,
                    title = stringResource(R.string.settings_dynamic_colors_title),
                    subtitle = stringResource(R.string.not_supported),
                    onClick = { /* TODO: Toggle dynamic color */ }
                )
            }

            // --- Section: Developer / Other ---
            item { SectionHeader(title = stringResource(R.string.settings_category_developer)) }

            item {
                SettingItem(
                    icon = Icons.Default.AccountCircle,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = "Achmad Daniel Syahputra",
                    onClick = { /* TODO: Buka profil developer */ }
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.Gavel,
                    title = stringResource(R.string.settings_license_title),
                    subtitle = stringResource(R.string.settings_license_summary),
                    onClick = { /* TODO: Buka lisensi */ }
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.app_name),
                    subtitle = "Version 3.0.1",
                    onClick = { /* TODO: Buka GitHub KuPass */ }
                )
            }
        }
    }
}