package com.nielcode.kupass.update

import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the update dialog shows. */
sealed interface UpdateState {
    /** The release in play; null while idle. */
    val update: AvailableUpdate?

    data object Idle : UpdateState {
        override val update: AvailableUpdate? = null
    }

    data class Available(override val update: AvailableUpdate) : UpdateState

    data class Downloading(override val update: AvailableUpdate, val percent: Int) : UpdateState

    /** Downloaded and verified; Kupass may not install apps yet. */
    data class NeedsPermission(override val update: AvailableUpdate, val apk: File) : UpdateState

    data class Installing(override val update: AvailableUpdate) : UpdateState

    data class Failed(override val update: AvailableUpdate) : UpdateState
}

/**
 * The in-app update flow, app-wide so a download survives rotation: offer at app open, download,
 * verify, ask for the install permission when needed, install.
 */
internal class UpdateManager(
    private val offer: () -> AvailableUpdate?,
    private val skip: (AvailableUpdate) -> Unit,
    private val download: suspend (AvailableUpdate, (Int) -> Unit) -> File?,
    private val installer: PackageUpdater,
    private val scope: CoroutineScope,
    private val io: CoroutineContext = Dispatchers.IO,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var job: Job? = null

    /** This process has already handled an app open. */
    private var opened = false

    /**
     * Called when the app opens. May request the release list.
     *
     * @param restored the screen was recreated from saved state: a rotation, or a restart of the
     *   app process (Android may restart it when "Install unknown apps" changes). Only the restart
     *   checks again.
     */
    fun onAppOpen(restored: Boolean = false) {
        if (restored && opened) return
        opened = true
        if (_state.value != UpdateState.Idle || job?.isActive == true) return
        job = scope.launch {
            val update = withContext(io) { offer() } ?: return@launch
            _state.compareAndSet(UpdateState.Idle, UpdateState.Available(update))
        }
    }

    /** "Later": hide it and don't offer this release again. */
    fun later() {
        (_state.value as? UpdateState.Available)?.let { skip(it.update) }
        close()
    }

    /** Back or a tap outside: hide it until the app opens again. */
    fun close() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }

    /** "Download" or "Try again". */
    fun download() {
        val update =
            when (val current = _state.value) {
                is UpdateState.Available -> current.update
                is UpdateState.Failed -> current.update
                else -> return
            }
        _state.value = UpdateState.Downloading(update, 0)
        job = scope.launch {
            val apk = withContext(io) { download(update) { showProgress(update, it) } }
            when {
                apk == null -> _state.value = UpdateState.Failed(update)
                installer.canInstall() -> install(update, apk)
                else -> _state.value = UpdateState.NeedsPermission(update, apk)
            }
        }
    }

    /** Stops a running download; the partial file is deleted. */
    fun cancelDownload() {
        val current = _state.value as? UpdateState.Downloading ?: return
        job?.cancel()
        _state.value = UpdateState.Available(current.update)
    }

    /** Back from the system screen: install if the permission was granted there. */
    fun onResume() {
        val current = _state.value as? UpdateState.NeedsPermission ?: return
        if (installer.canInstall()) install(current.update, current.apk)
    }

    /** The system installer reported a failure or the user declined. */
    fun onInstallFailed() {
        _state.update { if (it is UpdateState.Installing) UpdateState.Failed(it.update) else it }
    }

    /** Progress after a cancel must not bring the download back. */
    private fun showProgress(update: AvailableUpdate, percent: Int) {
        _state.update {
            if (it is UpdateState.Downloading && it.update == update) it.copy(percent = percent)
            else it
        }
    }

    /** Installing is set first, so a second resume can't start another session. */
    private fun install(update: AvailableUpdate, apk: File) {
        _state.value = UpdateState.Installing(update)
        job = scope.launch {
            if (!withContext(io) { installer.install(apk) }) {
                _state.value = UpdateState.Failed(update)
            }
        }
    }
}
