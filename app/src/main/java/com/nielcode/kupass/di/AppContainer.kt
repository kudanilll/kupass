package com.nielcode.kupass.di

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.nielcode.kupass.App
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.AppLock

/**
 * Manual dependency container, created once in [App.onCreate]. Kept deliberately small instead of
 * adding a DI framework, to keep the app light.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val passwordRepository: PasswordRepository by lazy {
        PasswordRepository(KupassDatabase.getInstance(appContext).passwordDao())
    }

    val preferenceManager: PreferenceManager by lazy { PreferenceManager(appContext) }

    /** Process-wide vault lock; survives activity recreation, resets (locked) with the process. */
    val appLock: AppLock by lazy { AppLock(timeoutMillis = { preferenceManager.autoLockSeconds * 1000L }) }

    val contentResolver: ContentResolver
        get() = appContext.contentResolver
}

/** Resolves the [AppContainer] inside a `viewModelFactory { initializer { … } }` block. */
fun CreationExtras.appContainer(): AppContainer = (checkNotNull(this[APPLICATION_KEY]) as App).container
