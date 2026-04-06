package com.nielcode.kupass.ui.screens.settings

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.MainActivity
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.ui.screens.settings.components.SectionHeader
import com.nielcode.kupass.ui.screens.settings.components.SettingItem
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {

    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }

    var currentLanguageIndex by remember { mutableIntStateOf(prefs.language) }
    var currentThemeIndex by remember { mutableIntStateOf(prefs.theme) }
    var currentDynamicColor by remember { mutableIntStateOf(prefs.dynamicColor) }

    // State Variables For Visibility
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDynamicColorsDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val languageList = stringArrayResource(id = R.array.language_list)
    val themeList = stringArrayResource(id = R.array.theme_list)

    val isDynamicColorSupported = DynamicColors.isDynamicColorAvailable()
    val dynamicColorStatus = when {
        !isDynamicColorSupported -> stringResource(R.string.not_supported)
        currentDynamicColor == AppConfig.DynamicColors.Code.ENABLE -> stringResource(R.string.enable)
        else -> stringResource(R.string.disable)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        val bottomPadding = innerPadding.calculateBottomPadding() + 88.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 32.dp,
                bottom = bottomPadding
            )
        ) {
            // Section: General
            item { SectionHeader(title = stringResource(R.string.settings)) }
            item {
                SettingItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = languageList.getOrNull(currentLanguageIndex)
                        ?: stringResource(R.string.lang_en),
                    onClick = { showLanguageDialog = true }
                )
            }

            // Section: Appearance
            item { SectionHeader(title = stringResource(R.string.settings_category_appearance)) }
            item {
                SettingItem(
                    icon = Icons.Default.Contrast,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = themeList.getOrNull(currentThemeIndex) ?: "System Default",
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingItem(
                    icon = Icons.Default.ColorLens,
                    title = stringResource(R.string.settings_dynamic_colors_title),
                    subtitle = dynamicColorStatus,
                    onClick = { if (isDynamicColorSupported) showDynamicColorsDialog = true }
                )
            }

            // Section: About
            item { SectionHeader(title = stringResource(R.string.settings_category_developer)) }
            item {
                SettingItem(
                    icon = Icons.Default.AccountCircle,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = BuildConfig.DEV_NAME,
                    onClick = { openUrl(context, BuildConfig.DEV_URL) }
                )
            }
            item {
                SettingItem(
                    icon = Icons.Default.Gavel,
                    title = stringResource(R.string.settings_license_title),
                    subtitle = stringResource(R.string.settings_license_summary),
                    onClick = {
                        val intent = Intent(context, OssLicensesMenuActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            item {
                SettingItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.app_name),
                    subtitle = "${BuildConfig.VERSION_NAME} - ${BuildConfig.BUILD_TYPE}",
                    onClick = { openUrl(context, BuildConfig.GIT_URL) }
                )
            }
        }

        val languageValues = stringArrayResource(id = R.array.language_values)

        if (showLanguageDialog) {
            SingleChoiceDialog(
                title = stringResource(id = R.string.settings_language_title),
                options = languageList.toList(),
                selectedIndex = currentLanguageIndex,
                onDismiss = { showLanguageDialog = false },
                onConfirm = { newIndex ->
                    if (currentLanguageIndex != newIndex) {
                        prefs.language = newIndex
                        currentLanguageIndex = newIndex
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(languageValues[newIndex])
                        )
                    }
                    showLanguageDialog = false
                }
            )
        }

        if (showThemeDialog) {
            SingleChoiceDialog(
                title = stringResource(id = R.string.settings_theme_title),
                options = themeList.toList(),
                selectedIndex = currentThemeIndex,
                onDismiss = { showThemeDialog = false },
                onConfirm = { newIndex ->
                    if (currentThemeIndex != newIndex) {
                        prefs.theme = newIndex
                        currentThemeIndex = newIndex
                        val nightMode = when (newIndex) {
                            AppConfig.Theme.Code.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                            AppConfig.Theme.Code.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                    }
                    showThemeDialog = false
                }
            )
        }

        if (showDynamicColorsDialog) {
            val options = listOf(stringResource(R.string.enable), stringResource(R.string.disable))
            val currentIndex =
                if (currentDynamicColor == AppConfig.DynamicColors.Code.ENABLE) 0 else 1

            SingleChoiceDialog(
                title = stringResource(R.string.settings_dynamic_colors_title),
                options = options,
                selectedIndex = currentIndex,
                onDismiss = { showDynamicColorsDialog = false },
                onConfirm = { newIndex ->
                    val newSetting =
                        if (newIndex == 0) AppConfig.DynamicColors.Code.ENABLE else AppConfig.DynamicColors.Code.DISABLE
                    if (currentDynamicColor != newSetting) {
                        prefs.dynamicColor = newSetting
                        currentDynamicColor = newSetting
                        showRestartDialog = true
                    }
                    showDynamicColorsDialog = false
                }
            )
        }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                text = { Text(stringResource(R.string.dialog_message_language)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRestartDialog = false
                        val restart = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(restart)
                    }) {
                        Text(stringResource(R.string.button_restart))
                    }
                }
            )
        }
    }
}


@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var tempSelectedIndex by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (index == tempSelectedIndex),
                                onClick = { tempSelectedIndex = index }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == tempSelectedIndex),
                            onClick = { tempSelectedIndex = index }
                        )
                        Text(text = option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelectedIndex) }) {
                Text(stringResource(R.string.button_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}