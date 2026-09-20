package com.nielcode.kupass

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.di.AppContainer
import com.nielcode.kupass.ui.theme.AppearanceSettings
import com.nielcode.kupass.utils.AppConfig
import kotlinx.coroutines.launch

class App : Application() {

    /** Application-wide dependencies. See [AppContainer]. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Bring rows written by older builds up to full-field v2 encryption. Retried next launch on
        // failure.
        container.applicationScope.launch {
            runCatching { container.passwordRepository.upgradeStoredFormat() }
        }

        // Block screenshots for all Activities
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    activity.window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE,
                    )
                }

                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {}
            }
        )

        val prefs = container.preferenceManager

        // Apply user settings on app startup
        AppearanceSettings.applyLanguage(this, prefs.language)
        AppearanceSettings.applyTheme(prefs.theme)
        applyDynamicColors(prefs)
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
