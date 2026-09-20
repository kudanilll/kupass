package com.nielcode.kupass.di

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.nielcode.kupass.App
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.data.siteicon.FavgetIconSource
import com.nielcode.kupass.data.siteicon.SiteIconRepository
import com.nielcode.kupass.security.AppLock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container, created once in [App.onCreate]. Kept deliberately small instead of
 * adding a DI framework, to keep the app light.
 */
class AppContainer(
    context: Context,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val appContext = context.applicationContext

    /** Process-lifetime scope for background maintenance (never tied to a screen). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + backgroundDispatcher)

    val passwordRepository: PasswordRepository by lazy {
        PasswordRepository(KupassDatabase.getInstance(appContext).passwordDao())
    }

    val preferenceManager: PreferenceManager by lazy { PreferenceManager(appContext) }

    /** Process-wide vault lock; survives activity recreation, resets (locked) with the process. */
    val appLock: AppLock by lazy {
        AppLock(timeoutMillis = { preferenceManager.autoLockSeconds * 1000L })
    }

    /** Opt-in site icons; unavailable when the build has no Favget API key. */
    val siteIcons: SiteIconRepository by lazy {
        SiteIconRepository(
            source =
                BuildConfig.FAVGET_API_KEY.takeIf { it.isNotEmpty() }
                    ?.let { FavgetIconSource(BuildConfig.FAVGET_API_URL, it) },
            enabledSetting = preferenceManager::siteIconsEnabled,
            scope = applicationScope,
        )
    }

    val contentResolver: ContentResolver
        get() = appContext.contentResolver
}

/** Resolves the [AppContainer] inside a `viewModelFactory { initializer { … } }` block. */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as App).container
