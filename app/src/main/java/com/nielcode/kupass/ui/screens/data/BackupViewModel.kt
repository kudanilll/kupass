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
) : ViewModel() {

    private val _events = Channel<VaultEvent>(Channel.BUFFERED)

    /** One-shot results for the UI; each event is delivered once. */
    val events: Flow<VaultEvent> = _events.receiveAsFlow()

    /** Backup password held only between the password dialog and the file picker result. */
    private var pendingExportPassword: CharArray? = null
    private var importJob: Job? = null

    private val _importPrompt = MutableStateFlow<ImportPrompt?>(null)

    /** Non-null while an encrypted backup is waiting for its password. */
    val importPrompt: StateFlow<ImportPrompt?> = _importPrompt.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)

    /** True during reads, parsing, crypto, and writes; blocks duplicate operations. */
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    /** Step 1 of export: remember the chosen password until the user picks a file. */
    fun prepareExport(password: CharArray) {
        if (
            _backupBusy.value ||
                _importPrompt.value != null ||
                password.size < BackupCodec.MIN_PASSWORD_LENGTH
        ) {
            password.fill('\u0000')
            return
        }
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = password
    }

    /** The user dismissed the file picker. */
    fun cancelExport() {
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
    }

    /** Step 2 of export: write an encrypted v2 backup to [uri]. */
    fun exportPasswords(uri: Uri) {
        if (_backupBusy.value || _importPrompt.value != null) return
        val password = pendingExportPassword ?: return
        pendingExportPassword = null
        _backupBusy.value = true
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                _events.send(recoverable { writeBackup(uri, password) } ?: VaultEvent.ExportFailed)
            } catch (_: BackupException) {
                _events.send(VaultEvent.ExportFailed)
            } finally {
                password.fill('\u0000')
                _backupBusy.value = false
            }
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
        if (_backupBusy.value || _importPrompt.value != null || pendingExportPassword != null)
            return
        _backupBusy.value = true
        importJob =
            viewModelScope
                .launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        val content = recoverable { withContext(ioDispatcher) { readBounded(uri) } }
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

    /** The stream closes on success, invalid UTF-8, a size limit, or a read failure. */
    private fun readBounded(uri: Uri): String {
        val stream = contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
        return stream.use(BackupInput::read)
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
    }
}
