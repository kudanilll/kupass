package com.nielcode.kupass.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.App
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.MainActivity
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.security.AppLock
import com.nielcode.kupass.ui.components.SectionHeader
import com.nielcode.kupass.ui.components.SectionItem
import com.nielcode.kupass.ui.components.SingleChoiceDialog
import com.nielcode.kupass.ui.screens.pagerPageInsets
import com.nielcode.kupass.ui.theme.AppearanceSettings
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.openUrl

private const val SECONDS_PER_MINUTE = 60

private enum class SettingsDialog {
    Language,
    AutoLock,
    Theme,
    DynamicColor,
    Restart,
}

/** App settings: language, auto-lock, theme, dynamic color, and about links. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as App).container }
    val settings = remember(container) { SettingsController(context, container.preferenceManager) }
    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(modifier = modifier.fillMaxSize(), contentWindowInsets = pagerPageInsets()) {
        innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    // Inside the scroll: the last item can scroll above the floating navigation.
                    .padding(top = 16.dp, bottom = 16.dp + contentPadding.calculateBottomPadding())
        ) {
            GeneralSection(
                languageIndex = settings.language,
                onLanguageClick = { openDialog = SettingsDialog.Language },
            )
            SecuritySection(
                autoLockSeconds = settings.autoLockSeconds,
                onAutoLockClick = { openDialog = SettingsDialog.AutoLock },
            )
            AppearanceSection(
                themeIndex = settings.theme,
                dynamicColor = settings.dynamicColor,
                onThemeClick = { openDialog = SettingsDialog.Theme },
                onDynamicColorClick = { openDialog = SettingsDialog.DynamicColor },
            )
            AboutSection(
                onOpen = { launch ->
                    // Leaving for a browser or the licenses screen must not lock the vault.
                    container.appLock.allowNextBackground()
                    launch(context)
                }
            )
        }
    }

    openDialog?.let { dialog ->
        SettingsDialogHost(
            dialog = dialog,
            settings = settings,
            onDismiss = { openDialog = null },
            onRestartRequest = { openDialog = SettingsDialog.Restart },
        )
    }
}

/** Current settings values, persisted and applied as soon as they change. */
@Stable
private class SettingsController(
    private val context: Context,
    private val prefs: PreferenceManager,
) {
    var language by mutableIntStateOf(prefs.language)
        private set

    var theme by mutableIntStateOf(prefs.theme)
        private set

    var dynamicColor by mutableIntStateOf(prefs.dynamicColor)
        private set

    var autoLockSeconds by mutableIntStateOf(prefs.autoLockSeconds)
        private set

    fun selectLanguage(index: Int) {
        if (index == language) return
        prefs.language = index
        language = index
        AppearanceSettings.applyLanguage(context, index)
    }

    fun selectTheme(code: Int) {
        if (code == theme) return
        prefs.theme = code
        theme = code
        AppearanceSettings.applyTheme(code)
    }

    /** @return true if the change needs an app restart to take effect. */
    fun selectDynamicColor(enabled: Boolean): Boolean {
        val code =
            if (enabled) AppConfig.DynamicColors.Code.ENABLE
            else AppConfig.DynamicColors.Code.DISABLE
        if (code == dynamicColor) return false
        prefs.dynamicColor = code
        dynamicColor = code
        return true
    }

    fun selectAutoLockSeconds(seconds: Int) {
        prefs.autoLockSeconds = seconds
        autoLockSeconds = seconds
    }
}

@Composable
private fun GeneralSection(
    languageIndex: Int,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val languages = stringArrayResource(R.array.language_list)
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings))
        SectionItem(
            icon = Icons.Default.Language,
            title = stringResource(R.string.settings_language_title),
            subtitle = languages.getOrNull(languageIndex) ?: stringResource(R.string.lang_en),
            onClick = onLanguageClick,
        )
    }
}

@Composable
private fun SecuritySection(
    autoLockSeconds: Int,
    onAutoLockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_category_security))
        SectionItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.settings_auto_lock_title),
            subtitle = autoLockLabel(autoLockSeconds),
            onClick = onAutoLockClick,
        )
    }
}

@Composable
private fun AppearanceSection(
    themeIndex: Int,
    dynamicColor: Int,
    onThemeClick: () -> Unit,
    onDynamicColorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = stringArrayResource(R.array.theme_list)
    val dynamicColorSupported = DynamicColors.isDynamicColorAvailable()
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_category_appearance))
        SectionItem(
            icon = Icons.Default.Contrast,
            title = stringResource(R.string.settings_theme_title),
            subtitle = themes.getOrNull(themeIndex) ?: stringResource(R.string.theme_default),
            onClick = onThemeClick,
        )
        SectionItem(
            icon = Icons.Default.ColorLens,
            title = stringResource(R.string.settings_dynamic_colors_title),
            subtitle =
                stringResource(
                    when {
                        !dynamicColorSupported -> R.string.not_supported
                        dynamicColor == AppConfig.DynamicColors.Code.ENABLE -> R.string.enable
                        else -> R.string.disable
                    }
                ),
            onClick = { if (dynamicColorSupported) onDynamicColorClick() },
        )
    }
}

/** @param onOpen starts an external screen; receives the launch action to run. */
@Composable
private fun AboutSection(onOpen: ((Context) -> Unit) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_category_developer))
        SectionItem(
            icon = Icons.Default.AccountCircle,
            title = stringResource(R.string.settings_about_title),
            subtitle = BuildConfig.DEV_NAME,
            onClick = { onOpen { openUrl(it, BuildConfig.DEV_URL) } },
        )
        SectionItem(
            icon = Icons.Default.Gavel,
            title = stringResource(R.string.settings_license_title),
            subtitle = stringResource(R.string.settings_license_summary),
            onClick = {
                onOpen { it.startActivity(Intent(it, OssLicensesMenuActivity::class.java)) }
            },
        )
        SectionItem(
            icon = Icons.Default.Code,
            title = stringResource(R.string.app_name),
            subtitle = "${BuildConfig.VERSION_NAME} - ${BuildConfig.BUILD_TYPE}",
            onClick = { onOpen { openUrl(it, BuildConfig.GIT_URL) } },
        )
    }
}

@Composable
private fun SettingsDialogHost(
    dialog: SettingsDialog,
    settings: SettingsController,
    onDismiss: () -> Unit,
    onRestartRequest: () -> Unit,
) {
    when (dialog) {
        SettingsDialog.Language ->
            SingleChoiceDialog(
                title = stringResource(R.string.settings_language_title),
                options = stringArrayResource(R.array.language_list).toList(),
                selectedIndex = settings.language,
                onDismiss = onDismiss,
                onConfirm = {
                    settings.selectLanguage(it)
                    onDismiss()
                },
            )
        SettingsDialog.AutoLock ->
            SingleChoiceDialog(
                title = stringResource(R.string.settings_auto_lock_title),
                options = AppLock.TIMEOUT_OPTIONS_SECONDS.map { autoLockLabel(it) },
                selectedIndex =
                    AppLock.TIMEOUT_OPTIONS_SECONDS.indexOf(settings.autoLockSeconds)
                        .coerceAtLeast(0),
                onDismiss = onDismiss,
                onConfirm = {
                    settings.selectAutoLockSeconds(AppLock.TIMEOUT_OPTIONS_SECONDS[it])
                    onDismiss()
                },
            )
        SettingsDialog.Theme ->
            SingleChoiceDialog(
                title = stringResource(R.string.settings_theme_title),
                options = stringArrayResource(R.array.theme_list).toList(),
                selectedIndex = settings.theme,
                onDismiss = onDismiss,
                onConfirm = {
                    settings.selectTheme(it)
                    onDismiss()
                },
            )
        SettingsDialog.DynamicColor ->
            SingleChoiceDialog(
                title = stringResource(R.string.settings_dynamic_colors_title),
                options = listOf(stringResource(R.string.enable), stringResource(R.string.disable)),
                selectedIndex =
                    if (settings.dynamicColor == AppConfig.DynamicColors.Code.ENABLE) 0 else 1,
                onDismiss = onDismiss,
                onConfirm = {
                    if (settings.selectDynamicColor(enabled = it == 0)) onRestartRequest()
                    else onDismiss()
                },
            )
        SettingsDialog.Restart -> RestartDialog()
    }
}

/** Dynamic colors only apply after a restart; this dialog can't be dismissed without one. */
@Composable
private fun RestartDialog(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        text = { Text(stringResource(R.string.dialog_message_language)) },
        confirmButton = {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            ) {
                Text(stringResource(R.string.button_restart))
            }
        },
    )
}

@Composable
private fun autoLockLabel(seconds: Int): String =
    when {
        seconds == 0 -> stringResource(R.string.auto_lock_immediately)
        seconds < SECONDS_PER_MINUTE ->
            pluralStringResource(R.plurals.auto_lock_seconds, seconds, seconds)
        else -> {
            val minutes = seconds / SECONDS_PER_MINUTE
            pluralStringResource(R.plurals.auto_lock_minutes, minutes, minutes)
        }
    }
