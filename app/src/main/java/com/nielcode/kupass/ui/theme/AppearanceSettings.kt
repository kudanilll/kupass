package com.nielcode.kupass.ui.theme

import android.content.Context
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

    /** Applies the night mode for an [AppConfig.Theme.Code] value. */
    fun applyTheme(themeCode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            when (themeCode) {
                AppConfig.Theme.Code.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppConfig.Theme.Code.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
