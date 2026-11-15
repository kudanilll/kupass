package com.nielcode.kupass.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.nielcode.kupass.R
import com.nielcode.kupass.utils.AppConfig

/** Applies the saved language and theme, shared by app startup and the Settings screen. */
object AppearanceSettings {

    /**
     * Switches the per-app locale to the language at [languageIndex] of `R.array.language_values`.
     * Out-of-range indexes (e.g. localized arrays drifting apart) are ignored instead of crashing.
     */
    fun applyLanguage(context: Context, languageIndex: Int) {
        val tag = context.resources.getStringArray(R.array.language_values).getOrNull(languageIndex)
        if (tag != null)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /**
     * Applies the night mode for an [AppConfig.Theme.Code] value. From Android 12 the system keeps
     * it for Kupass, so the splash screen it draws before Kupass runs is in the same mode, and
     * AppCompat follows the configuration the system then gives Kupass.
     */
    fun applyTheme(context: Context, themeCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context
                .getSystemService(UiModeManager::class.java)
                .setApplicationNightMode(
                    when (themeCode) {
                        AppConfig.Theme.Code.LIGHT -> UiModeManager.MODE_NIGHT_NO
                        AppConfig.Theme.Code.DARK -> UiModeManager.MODE_NIGHT_YES
                        // For an app, AUTO clears its own mode, so it follows the system again.
                        else -> UiModeManager.MODE_NIGHT_AUTO
                    }
                )
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else {
            AppCompatDelegate.setDefaultNightMode(
                when (themeCode) {
                    AppConfig.Theme.Code.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppConfig.Theme.Code.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }
}
