package com.nielcode.kupass

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.data.CoreContainer
import com.nielcode.kupass.data.CoreContainerProvider
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.di.AppContainer
import com.nielcode.kupass.ui.theme.AppearanceSettings
import com.nielcode.kupass.utils.AppConfig
import kotlinx.coroutines.launch

class App : Application(), CoreContainerProvider {

    /** Application-wide dependencies. See [AppContainer]. */
    lateinit var container: AppContainer
        private set

    override val coreContainer: CoreContainer
        get() = container

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Bring rows written by older builds up to full-field v2 encryption. Retried next launch on
        // failure.
        container.applicationScope.launch {
            runCatching { container.passwordRepository.upgradeStoredFormat() }
        }

        // Block screenshots for all Activities. Debug builds allow them for now, for development
        // (Danil, 2026-11-16; CONCERNS §1). Release builds always block them.
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (BuildConfig.DEBUG) return
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
        AppearanceSettings.applyTheme(this, prefs.theme)
        applyDynamicColors(prefs)
    }

    /**
     * Colors the activity windows (what shows before and around Compose). The Compose UI gets its
     * dynamic scheme separately, from `KupassTheme(dynamicColor = …)`; both read the same setting.
     */
    private fun applyDynamicColors(prefs: PreferenceManager) {
        if (DynamicColors.isDynamicColorAvailable()) {
            // Asked for each new activity, so a change in Settings reaches the next one.
            DynamicColors.applyToActivitiesIfAvailable(this) { _, _ ->
                prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE
            }
        } else {
            // If the device does not support it, ensure the preference reflects this.
            prefs.dynamicColor = AppConfig.DynamicColors.Code.NOT_SUPPORTED
        }
    }
}
