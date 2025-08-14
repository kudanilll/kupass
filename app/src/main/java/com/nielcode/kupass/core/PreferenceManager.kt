package com.nielcode.kupass.core

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

    var exportMethod: Int
        get() = sharedPreferences.getInt(KEY_EXPORT_METHOD, AppConfig.FileType.TEXT)
        set(value) = edit { putInt(KEY_EXPORT_METHOD, value) }

    var importMethod: Int
        get() = sharedPreferences.getInt(KEY_IMPORT_METHOD, AppConfig.FileType.JSON)
        set(value) = edit { putInt(KEY_IMPORT_METHOD, value) }

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
        private const val KEY_EXPORT_METHOD = "export_method"
        private const val KEY_IMPORT_METHOD = "import_method"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}