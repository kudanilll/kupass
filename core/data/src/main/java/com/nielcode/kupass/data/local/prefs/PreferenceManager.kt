package com.nielcode.kupass.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.nielcode.kupass.security.AppLock
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
        get() = sharedPreferences.getInt(KEY_DYNAMIC_COLOR, AppConfig.DynamicColors.Code.ENABLE)
        set(value) = edit { putInt(KEY_DYNAMIC_COLOR, value) }

    /** Auto-lock delay after leaving the app, in seconds (0 = immediately). */
    var autoLockSeconds: Int
        get() = sharedPreferences.getInt(KEY_AUTO_LOCK_SECONDS, AppLock.DEFAULT_TIMEOUT_SECONDS)
        set(value) = edit { putInt(KEY_AUTO_LOCK_SECONDS, value) }

    var excludeLookalike: Boolean
        get() = sharedPreferences.getBoolean(KEY_EXCLUDE_LOOKALIKE, false)
        set(value) = edit { putBoolean(KEY_EXCLUDE_LOOKALIKE, value) }

    /**
     * Autofill offers to save sign-ins Kupass doesn't know yet. Generated passwords always save.
     */
    var offerToSaveLogins: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTOFILL_OFFER_SAVE, true)
        set(value) = edit { putBoolean(KEY_AUTOFILL_OFFER_SAVE, value) }

    /** Kupass looks for a newer release when it opens, at most once a day. */
    var checkForUpdates: Boolean
        get() = sharedPreferences.getBoolean(KEY_UPDATE_CHECK, true)
        set(value) = edit { putBoolean(KEY_UPDATE_CHECK, value) }

    /** When the release list was last requested (epoch millis), whether or not that worked. */
    var lastUpdateCheckMillis: Long
        get() = sharedPreferences.getLong(KEY_UPDATE_LAST_CHECK, 0L)
        set(value) = edit { putLong(KEY_UPDATE_LAST_CHECK, value) }

    /** The newer release the last check found, as JSON; null when there is none. */
    var pendingUpdate: String?
        get() = sharedPreferences.getString(KEY_UPDATE_PENDING, null)
        set(value) = edit { putString(KEY_UPDATE_PENDING, value) }

    /** The release tag the user postponed with "Later"; it isn't offered again. */
    var skippedUpdateTag: String?
        get() = sharedPreferences.getString(KEY_UPDATE_SKIPPED, null)
        set(value) = edit { putString(KEY_UPDATE_SKIPPED, value) }

    companion object {
        private const val PREF_NAME = "kupass_preferences"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_AUTO_LOCK_SECONDS = "auto_lock_seconds"
        private const val KEY_EXCLUDE_LOOKALIKE = "exclude_lookalike"
        private const val KEY_AUTOFILL_OFFER_SAVE = "autofill_offer_save"
        private const val KEY_UPDATE_CHECK = "update_check"
        private const val KEY_UPDATE_LAST_CHECK = "update_last_check"
        private const val KEY_UPDATE_PENDING = "update_pending"
        private const val KEY_UPDATE_SKIPPED = "update_skipped"
    }
}
