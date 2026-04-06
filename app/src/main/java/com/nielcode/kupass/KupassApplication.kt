package com.nielcode.kupass

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.utils.AppConfig

class KupassApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Block screenshots for all Activities
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        val prefs = PreferenceManager(this)

        // Apply user settings on app startup
        applyLanguage(prefs.language)
        applyTheme(prefs.theme)
        applyDynamicColors(prefs)
    }

    private fun applyLanguage(languageIndex: Int) {
        val languageTags = resources.getStringArray(R.array.language_values)
        // Ensure index is valid to prevent crashes
        if (languageIndex in languageTags.indices) {
            val localeTag = languageTags[languageIndex]
            val appLocale = LocaleListCompat.forLanguageTags(localeTag)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun applyTheme(themeCode: Int) {
        val nightMode = when (themeCode) {
            AppConfig.Theme.Code.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppConfig.Theme.Code.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun applyDynamicColors(prefs: PreferenceManager) {
        if (DynamicColors.isDynamicColorAvailable()) {
            // Apply dynamic colors only if it's enabled by the user.
            if (prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE) {
                DynamicColors.applyToActivitiesIfAvailable(this)
            }
        } else {
            // If the device does not support it, ensure the preference reflects this.
            prefs.dynamicColor = AppConfig.DynamicColors.Code.NOT_SUPPORTED
        }
    }
}