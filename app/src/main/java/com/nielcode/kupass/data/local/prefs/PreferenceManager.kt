package com.nielcode.kupass.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.nielcode.kupass.utils.AppConfig

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // A helper function to avoid repeating 'sharedPreferences.edit()'.
    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sharedPreferences.edit().apply(block).apply()
    }

    var language: Int
        get() = sharedPreferences.getInt(KEY_LANGUAGE, AppConfig.Language.Code.ENGLISH)
        set(value) = edit { putInt(KEY_LANGUAGE, value) }

    var theme: Int
        get() = sharedPreferences.getInt(KEY_THEME, AppConfig.Theme.Code.SYSTEM)
        set(value) = edit { putInt(KEY_THEME, value) }

    var dynamicColor: Int
        get() = sharedPreferences.getInt(KEY_DYNAMIC_COLOR, AppConfig.DynamicColors.Code.DISABLE)
        set(value) = edit { putInt(KEY_DYNAMIC_COLOR, value) }

    companion object {
        private const val PREF_NAME = "kupass_preferences"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}