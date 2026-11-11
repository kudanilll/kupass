package com.nielcode.kupass.di

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.nielcode.kupass.App
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.data.CoreContainer
import com.nielcode.kupass.data.local.cache.SiteIconStore
import com.nielcode.kupass.data.local.cache.VaultSnapshotStore
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.data.siteicon.FavgetIconSource
import com.nielcode.kupass.data.siteicon.SiteIconRepository
import com.nielcode.kupass.update.AppVersion
import com.nielcode.kupass.update.UpdateChecker
import com.nielcode.kupass.update.UpdateDownloader
import com.nielcode.kupass.update.UpdateInstaller
import com.nielcode.kupass.update.UpdateManager
import com.nielcode.kupass.update.fetchReleases
import java.io.File
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
) : CoreContainer {
    private val appContext = context.applicationContext

    /** Process-lifetime scope for background maintenance (never tied to a screen). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + backgroundDispatcher)

    override val passwordRepository: PasswordRepository by lazy {
        PasswordRepository(
            KupassDatabase.getInstance(appContext).passwordDao(),
            snapshotStore = VaultSnapshotStore(File(appContext.noBackupFilesDir, "vault-cache")),
        )
    }

    override val preferenceManager: PreferenceManager by lazy { PreferenceManager(appContext) }

    /** Site icons; fetches gracefully return null when the build has no Favget API key. */
    val siteIcons: SiteIconRepository by lazy {
        SiteIconRepository(
            source =
                BuildConfig.FAVGET_API_KEY.takeIf { it.isNotEmpty() }
                    ?.let { FavgetIconSource(BuildConfig.FAVGET_API_URL, it) },
            scope = applicationScope,
            store = SiteIconStore(File(appContext.noBackupFilesDir, "site-icons")),
        )
    }

    val contentResolver: ContentResolver
        get() = appContext.contentResolver

    /** In-app updates from the release feed. Debug builds never check. */
    internal val updates: UpdateManager by lazy {
        val installed =
            if (BuildConfig.DEBUG) null
            else AppVersion.installed(BuildConfig.VERSION_NAME, BuildConfig.BETA_NUMBER)
        val checker =
            UpdateChecker(
                preferenceManager,
                installed,
                fetchReleases = { fetchReleases(BuildConfig.UPDATE_FEED_URL) },
            )
        UpdateManager(
            offer = checker::updateToOffer,
            skip = checker::skip,
            download = UpdateDownloader(File(appContext.cacheDir, "updates"))::download,
            installer = UpdateInstaller(appContext),
            scope = applicationScope,
        )
    }
}

/** Resolves the [AppContainer] inside a `viewModelFactory { initializer { … } }` block. */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as App).container
