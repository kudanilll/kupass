package com.nielcode.kupass.ui.screens.home

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.data.backup.BackupCodec
import com.nielcode.kupass.data.backup.BackupException
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.di.appContainer
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Home screen. Manages the password list, search, delete, and export/import
 * functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: PasswordRepository,
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class) val passwords: StateFlow<List<PasswordEntity>>

    private val _events = Channel<VaultEvent>(Channel.BUFFERED)

    /** One-shot results for the UI; each event is delivered once. */
    val events: Flow<VaultEvent> = _events.receiveAsFlow()

    init {
        passwords =
            _searchQuery
                .flatMapLatest { query ->
                    val source =
                        if (query.isBlank()) {
                            repository.getAllPasswords()
                        } else {
                            repository.searchPasswords(query)
                        }
                    // Never crash or show ciphertext when the vault can't be decrypted.
                    source.catch {
                        _events.trySend(VaultEvent.VaultReadFailed)
                        emit(emptyList())
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch { repository.deletePassword(password) }
    }

    /** Backup password held only between the password dialog and the file picker result. */
    private var pendingExportPassword: CharArray? = null

    private val _importPrompt = MutableStateFlow<ImportPrompt?>(null)

    /** Non-null while an encrypted backup is waiting for its password. */
    val importPrompt: StateFlow<ImportPrompt?> = _importPrompt.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)

    /** True while a backup is being encrypted or decrypted (PBKDF2 takes a moment). */
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    /** Step 1 of export: remember the chosen password until the user picks a file. */
    fun prepareExport(password: CharArray) {
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
        val password = pendingExportPassword ?: return
        pendingExportPassword = null
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val allPasswords = repository.getAllPasswords().first()
                if (allPasswords.isEmpty()) {
                    _events.send(VaultEvent.NothingToExport)
                    return@launch
                }
                val backup =
                    withContext(cpuDispatcher) { BackupCodec.encode(allPasswords, password) }
                withContext(ioDispatcher) {
                    val stream =
                        contentResolver.openOutputStream(uri, "wt")
                            ?: throw IOException("No output stream")
                    stream.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
                }
                _events.send(VaultEvent.ExportSucceeded)
            } catch (_: Exception) {
                _events.send(VaultEvent.ExportFailed)
            } finally {
                password.fill('\u0000')
                _backupBusy.value = false
            }
        }
    }

    /** Reads the backup at [uri]. Encrypted backups open the password prompt first. */
    fun importPasswords(uri: Uri) {
        viewModelScope.launch {
            val content =
                try {
                    withContext(ioDispatcher) { readBounded(uri) }
                } catch (_: BackupException.TooLarge) {
                    _events.send(VaultEvent.ImportTooLarge)
                    return@launch
                } catch (_: Exception) {
                    _events.send(VaultEvent.ImportFailed)
                    return@launch
                }
            if (BackupCodec.isPasswordProtected(content)) {
                _importPrompt.value = ImportPrompt(content)
            } else {
                decodeAndInsert(content, password = null)
            }
        }
    }

    /** Password entered for the pending encrypted backup. */
    fun submitImportPassword(password: CharArray) {
        val prompt = _importPrompt.value ?: return
        viewModelScope.launch { decodeAndInsert(prompt.content, password) }
    }

    fun cancelImport() {
        _importPrompt.value = null
    }

    private suspend fun decodeAndInsert(content: String, password: CharArray?) {
        _backupBusy.value = true
        try {
            val imported = withContext(cpuDispatcher) { BackupCodec.decode(content, password) }
            _importPrompt.value = null
            val result = repository.importPasswords(imported)
            _events.send(
                VaultEvent.ImportSucceeded(imported = result.imported, skipped = result.skipped)
            )
        } catch (_: BackupException.WrongPassword) {
            // Keep the prompt open so the user can retry.
            _importPrompt.value = _importPrompt.value?.copy(wrongPassword = true)
        } catch (e: BackupException) {
            _importPrompt.value = null
            _events.send(
                when (e) {
                    is BackupException.ForeignDevice -> VaultEvent.BackupFromOtherDevice
                    is BackupException.Unsupported -> VaultEvent.BackupUnsupported
                    is BackupException.TooLarge -> VaultEvent.ImportTooLarge
                    else -> VaultEvent.ImportFailed
                }
            )
        } catch (_: Exception) {
            _importPrompt.value = null
            _events.send(VaultEvent.ImportFailed)
        } finally {
            password?.fill('\u0000')
            _backupBusy.value = false
        }
    }

    /** Reads the file as UTF-8, refusing anything larger than [MAX_BACKUP_BYTES]. */
    private fun readBounded(uri: Uri): String {
        val stream = contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
        stream.use { input ->
            // Manual loop: InputStream.readNBytes is API 33+, minSdk is 27.
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > MAX_BACKUP_BYTES) throw BackupException.TooLarge()
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }

    override fun onCleared() {
        cancelExport()
        super.onCleared()
    }

    /** An encrypted backup waiting for its password. [content] is still ciphertext. */
    data class ImportPrompt(val content: String, val wrongPassword: Boolean = false)

    companion object {
        private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(container.passwordRepository, container.contentResolver)
            }
        }
    }
}
