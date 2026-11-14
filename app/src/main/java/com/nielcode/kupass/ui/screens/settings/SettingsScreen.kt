package com.nielcode.kupass.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.App
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.security.AppLock
import com.nielcode.kupass.ui.components.BottomFade
import com.nielcode.kupass.ui.components.SectionHeader
import com.nielcode.kupass.ui.components.SectionItem
import com.nielcode.kupass.ui.components.SingleChoiceDialog
import com.nielcode.kupass.ui.components.SwitchSectionItem
import com.nielcode.kupass.ui.components.pageVerticalScroll
import com.nielcode.kupass.ui.icons.KupassIcons
import com.nielcode.kupass.ui.screens.pagerPageInsets
import com.nielcode.kupass.ui.theme.AppearanceSettings
import com.nielcode.kupass.ui.theme.Typography
import com.nielcode.kupass.ui.theme.kupassDarkColorScheme
import com.nielcode.kupass.ui.theme.kupassLightColorScheme
import com.nielcode.kupass.update.CheckResult
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.openUrl
import kotlinx.coroutines.launch

private const val SECONDS_PER_MINUTE = 60

private enum class SettingsDialog {
    Language,
    AutoLock,
    Theme,
}

/** App settings: language, security, autofill, appearance, and about. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onAllowBackground: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings = rememberSettingsController(context)
    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(modifier = modifier.fillMaxSize(), contentWindowInsets = pagerPageInsets()) {
        innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .pageVerticalScroll()
                        // Inside the scroll: the last item can scroll above the floating
                        // navigation.
                        .padding(
                            top = 16.dp,
                            bottom = 16.dp + contentPadding.calculateBottomPadding(),
                        )
            ) {
                GeneralSection(
                    languageIndex = settings.language,
                    onLanguageClick = { openDialog = SettingsDialog.Language },
                )
                SecuritySection(
                    autoLockSeconds = settings.autoLockSeconds,
                    onAutoLockClick = { openDialog = SettingsDialog.AutoLock },
                    excludeLookalike = settings.excludeLookalike,
                    onExcludeLookalikeChange = settings::selectExcludeLookalike,
                )
                AutofillSection(
                    onAllowBackground = onAllowBackground,
                    offerToSave = settings.offerToSaveLogins,
                    onOfferToSaveChange = settings::selectOfferToSaveLogins,
                )
                AppearanceSection(
                    themeIndex = settings.theme,
                    dynamicColor = settings.dynamicColor,
                    onThemeClick = { openDialog = SettingsDialog.Theme },
                    onDynamicColorChange = settings::selectDynamicColor,
                )
                AboutSection(
                    settings = settings,
                    onOpen = { launch ->
                        // Use the launching activity's bounded allowance for external screens.
                        onAllowBackground()
                        launch(context)
                    },
                )
            }
            BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    openDialog?.let { dialog ->
        SettingsDialogHost(
            dialog = dialog,
            settings = settings,
            onDismiss = { openDialog = null },
        )
    }
}

@Composable
private fun rememberSettingsController(context: Context): SettingsController {
    val container = remember(context) { (context.applicationContext as App).container }
    return remember(container) {
        SettingsController(
            context,
            container.preferenceManager,
            container.dynamicColor,
            container.updates::checkNow,
        )
    }
}

/** Current settings values, persisted and applied as soon as they change. */
@Stable
internal class SettingsController(
    private val context: Context,
    private val prefs: PreferenceManager,
    private val appDynamicColor: MutableState<Boolean>,
    private val checkNow: suspend () -> CheckResult,
) {
    var language by mutableIntStateOf(prefs.language)
        private set

    var theme by mutableIntStateOf(prefs.theme)
        private set

    /** Read by the app theme too, so a change recolors the open screens at once. */
    val dynamicColor: Boolean
        get() = appDynamicColor.value

    var autoLockSeconds by mutableIntStateOf(prefs.autoLockSeconds)
        private set

    var excludeLookalike by mutableStateOf(prefs.excludeLookalike)
        private set

    var offerToSaveLogins by mutableStateOf(prefs.offerToSaveLogins)
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

    fun selectDynamicColor(enabled: Boolean) {
        if (enabled == dynamicColor) return
        prefs.dynamicColor =
            if (enabled) AppConfig.DynamicColors.Code.ENABLE
            else AppConfig.DynamicColors.Code.DISABLE
        appDynamicColor.value = enabled
    }

    fun selectAutoLockSeconds(seconds: Int) {
        prefs.autoLockSeconds = seconds
        autoLockSeconds = seconds
    }

    fun selectExcludeLookalike(enabled: Boolean) {
        if (enabled == excludeLookalike) return
        prefs.excludeLookalike = enabled
        excludeLookalike = enabled
    }

    fun selectOfferToSaveLogins(enabled: Boolean) {
        if (enabled == offerToSaveLogins) return
        prefs.offerToSaveLogins = enabled
        offerToSaveLogins = enabled
    }

    var checkingForUpdates by mutableStateOf(false)
        private set

    /**
     * Asks GitHub for a newer release now. A release it finds opens the update dialog.
     *
     * @return the message to show instead; null when the dialog opens or a check is running.
     */
    suspend fun checkForUpdates(): Int? {
        if (checkingForUpdates) return null
        checkingForUpdates = true
        return try {
            when (checkNow()) {
                is CheckResult.Found -> null
                CheckResult.UpToDate -> R.string.update_check_latest
                CheckResult.Failed -> R.string.update_check_failed
                CheckResult.Unavailable -> R.string.update_check_release_only
            }
        } finally {
            checkingForUpdates = false
        }
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
            icon = KupassIcons.Language,
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
    excludeLookalike: Boolean,
    onExcludeLookalikeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_category_security))
        SectionItem(
            icon = KupassIcons.Lock,
            title = stringResource(R.string.settings_auto_lock_title),
            subtitle = autoLockLabel(autoLockSeconds),
            onClick = onAutoLockClick,
        )
        SwitchSectionItem(
            icon = KupassIcons.Password,
            title = stringResource(R.string.settings_exclude_lookalike_title),
            subtitle = stringResource(R.string.settings_exclude_lookalike_summary),
            checked = excludeLookalike,
            onCheckedChange = onExcludeLookalikeChange,
        )
    }
}

@Composable
private fun AppearanceSection(
    themeIndex: Int,
    dynamicColor: Boolean,
    onThemeClick: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = stringArrayResource(R.array.theme_list)
    val dynamicColorSupported = DynamicColors.isDynamicColorAvailable()
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_category_appearance))
        SectionItem(
            icon = KupassIcons.Contrast,
            title = stringResource(R.string.settings_theme_title),
            subtitle = themes.getOrNull(themeIndex) ?: stringResource(R.string.theme_default),
            onClick = onThemeClick,
        )
        SwitchSectionItem(
            icon = KupassIcons.ColorLens,
            title = stringResource(R.string.settings_dynamic_colors_title),
            subtitle =
                stringResource(
                    if (dynamicColorSupported) R.string.settings_dynamic_colors_summary
                    else R.string.not_supported
                ),
            checked = dynamicColorSupported && dynamicColor,
            onCheckedChange = onDynamicColorChange,
            enabled = dynamicColorSupported,
        )
    }
}

/** @param onOpen starts an external screen; receives the launch action to run. */
@Composable
private fun AboutSection(
    settings: SettingsController,
    onOpen: ((Context) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.settings_category_developer))
        SectionItem(
            icon = KupassIcons.Gavel,
            title = stringResource(R.string.settings_license_title),
            subtitle = stringResource(R.string.settings_license_summary),
            onClick = {
                onOpen {
                    OssLicensesMenuActivity.setTheme(
                        kupassLightColorScheme(it, settings.dynamicColor),
                        kupassDarkColorScheme(it, settings.dynamicColor),
                        Typography,
                    )
                    it.startActivity(Intent(it, OssLicensesMenuActivity::class.java))
                }
            },
        )
        SectionItem(
            icon = KupassIcons.SystemUpdate,
            title = stringResource(R.string.settings_check_updates),
            subtitle =
                stringResource(
                    if (settings.checkingForUpdates) R.string.settings_check_updates_checking
                    else R.string.settings_check_updates_summary
                ),
            onClick = {
                scope.launch {
                    settings.checkForUpdates()?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
        SectionItem(
            icon = KupassIcons.Code,
            title = stringResource(R.string.app_name),
            subtitle = "${installedVersionLabel()} - ${BuildConfig.BUILD_TYPE}",
            onClick = { onOpen { openUrl(it, BuildConfig.GIT_URL) } },
        )
    }
}

@Composable
private fun SettingsDialogHost(
    dialog: SettingsDialog,
    settings: SettingsController,
    onDismiss: () -> Unit,
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
    }
}

/** `3.1.0`, or `3.1.0-beta.2` for a beta build, so testers can tell builds apart. */
private fun installedVersionLabel(): String =
    if (BuildConfig.BETA_NUMBER > 0) "${BuildConfig.VERSION_NAME}-beta.${BuildConfig.BETA_NUMBER}"
    else BuildConfig.VERSION_NAME

@Composable
@ReadOnlyComposable
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
