package com.nielcode.kupass.ui.screens.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.data.backup.BackupCodec
import com.nielcode.kupass.data.backup.BackupException
import com.nielcode.kupass.data.backup.BackupInput
import com.nielcode.kupass.data.backup.GooglePasswordCsv
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.di.appContainer
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.ExportConsent
import com.nielcode.kupass.ui.screens.VaultEvent
import com.nielcode.kupass.ui.screens.recoverable
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for backup export and import: holds the backup password only as long as needed, prompts
 * for the password of encrypted backups, and reports results as [VaultEvent]s.
 */
class BackupViewModel(
    private val repository: PasswordRepository,
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val authentication: EntryAuthenticationViewModel? = null,
) : ViewModel() {

    private val _events = Channel<VaultEvent>(Channel.BUFFERED)

    /** One-shot results for the UI; each event is delivered once. */
    val events: Flow<VaultEvent> = _events.receiveAsFlow()

    /** Backup password held only between the password dialog and the file picker result. */
    private var pendingExportPassword: CharArray? = null
    private var importJob: Job? = null
    private var pendingExportToken: Long? = null

    private val _importPrompt = MutableStateFlow<ImportPrompt?>(null)

    /** Non-null while an encrypted backup is waiting for its password. */
    val importPrompt: StateFlow<ImportPrompt?> = _importPrompt.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)

    /** True during reads, parsing, crypto, and writes; blocks duplicate operations. */
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    private val operationPending: Boolean
        get() = _backupBusy.value || _importPrompt.value != null

    init {
        authentication?.let { owner ->
            viewModelScope.launch {
                owner.state.collect { clearRevokedExport() }
            }
        }
    }

    /** Step 1 of export: remember the chosen password until the user picks a file. */
    fun prepareExport(password: CharArray) {
        val consent = authentication?.state?.value?.export
        if (
            consent?.phase != ExportConsent.Phase.Picker ||
                operationPending ||
                password.size < BackupCodec.MIN_PASSWORD_LENGTH
        ) {
            password.fill('\u0000')
            cancelExport()
            return
        }
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = password
        pendingExportToken = consent.token
    }

    /** The user dismissed the file picker. */
    fun cancelExport() {
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
        pendingExportToken = null
    }

    private fun clearRevokedExport() {
        val token = pendingExportToken ?: return
        if (authentication?.state?.value?.export?.token != token) cancelExport()
    }

    /** Step 2 of export: write an encrypted v2 backup to [uri]. */
    fun exportPasswords(uri: Uri) {
        if (operationPending) {
            cancelExport()
            return
        }
        val password = pendingExportPassword ?: return
        val token = pendingExportToken
        pendingExportPassword = null
        pendingExportToken = null
        if (authentication?.consumeExportForWrite(token) == true) {
            _backupBusy.value = true
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    _events.send(
                        recoverable { writeBackup(uri, password) } ?: VaultEvent.ExportFailed
                    )
                } catch (_: BackupException) {
                    _events.send(VaultEvent.ExportFailed)
                } finally {
                    password.fill('\u0000')
                    _backupBusy.value = false
                }
            }
        } else {
            password.fill('\u0000')
        }
    }

    private suspend fun writeBackup(uri: Uri, password: CharArray): VaultEvent {
        val allPasswords = repository.getAllPasswords().first()
        if (allPasswords.isEmpty()) return VaultEvent.NothingToExport
        val backup = withContext(cpuDispatcher) { BackupCodec.encode(allPasswords, password) }
        withContext(ioDispatcher) {
            val stream =
                contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
            stream.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
        }
        return VaultEvent.ExportSucceeded
    }

    /** Reads JSON backups or Google CSV by content, regardless of the provider's name or MIME. */
    fun importPasswords(uri: Uri) {
        clearRevokedExport()
        val authState = authentication?.state?.value
        val nativeExportPending =
            authState?.pending?.action == EntryAuthenticationViewModel.Action.Export ||
                authState?.nativeRequest?.action == EntryAuthenticationViewModel.Action.Export
        val exportPending =
            pendingExportPassword != null || authState?.export != null || nativeExportPending
        if (operationPending || exportPending) return
        _backupBusy.value = true
        importJob =
            viewModelScope
                .launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        val content = recoverable {
                            withContext(ioDispatcher) {
                                val stream =
                                    contentResolver.openInputStream(uri)
                                        ?: throw IOException("No input stream")
                                stream.use(BackupInput::read)
                            }
                        }
                        when {
                            content == null -> _events.send(VaultEvent.ImportFailed)
                            withContext(cpuDispatcher) {
                                BackupCodec.isPasswordProtected(content)
                            } -> _importPrompt.value = ImportPrompt(content)
                            else -> decodeAndInsert(content, password = null)
                        }
                    } catch (e: BackupException) {
                        _events.send(e.toEvent())
                    } finally {
                        _backupBusy.value = false
                        importJob = null
                    }
                }
                .takeIf { it.isActive }
    }

    /** Password entered for the pending encrypted backup. */
    fun submitImportPassword(password: CharArray) {
        val prompt = _importPrompt.value
        if (prompt == null || _backupBusy.value) {
            password.fill('\u0000')
            return
        }
        _backupBusy.value = true
        importJob =
            viewModelScope
                .launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        decodeAndInsert(prompt.content, password)
                    } finally {
                        password.fill('\u0000')
                        _backupBusy.value = false
                        importJob = null
                    }
                }
                .takeIf { it.isActive }
    }

    fun cancelImport() {
        _importPrompt.value = null
        importJob?.cancel()
    }

    private suspend fun decodeAndInsert(content: String, password: CharArray?) {
        try {
            val result = recoverable {
                val decoded =
                    withContext(cpuDispatcher) {
                        if (content.firstOrNull { !it.isWhitespace() } in listOf('[', '{')) {
                            GooglePasswordCsv.Result(
                                BackupCodec.decode(content, password),
                                skipped = 0,
                            )
                        } else GooglePasswordCsv.decode(content)
                    }
                val inserted = repository.importPasswords(decoded.entries)
                inserted.copy(skipped = inserted.skipped + decoded.skipped)
            }
            _importPrompt.value = null
            _events.send(
                result?.let {
                    VaultEvent.ImportSucceeded(imported = it.imported, skipped = it.skipped)
                } ?: VaultEvent.ImportFailed
            )
        } catch (_: BackupException.WrongPassword) {
            // Keep the prompt open so the user can retry.
            _importPrompt.value = _importPrompt.value?.copy(wrongPassword = true)
        } catch (e: BackupException) {
            _importPrompt.value = null
            _events.send(e.toEvent())
        }
    }

    private fun BackupException.toEvent(): VaultEvent =
        when (this) {
            is BackupException.ForeignDevice -> VaultEvent.BackupFromOtherDevice
            is BackupException.Unsupported -> VaultEvent.BackupUnsupported
            is BackupException.TooLarge -> VaultEvent.ImportTooLarge
            else -> VaultEvent.ImportFailed
        }

    override fun onCleared() {
        cancelExport()
        cancelImport()
    }

    /** An encrypted backup waiting for its password. [content] is still ciphertext. */
    data class ImportPrompt(val content: String, val wrongPassword: Boolean = false)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                BackupViewModel(container.passwordRepository, container.contentResolver)
            }
        }

        /** Native consent is activity-retained, never process-wide or saved. */
        fun factory(authentication: EntryAuthenticationViewModel): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = appContainer()
                    BackupViewModel(
                        container.passwordRepository,
                        container.contentResolver,
                        authentication = authentication,
                    )
                }
            }
    }
}
