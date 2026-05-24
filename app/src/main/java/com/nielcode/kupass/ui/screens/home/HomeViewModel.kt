package com.nielcode.kupass.ui.screens.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.local.json.JsonExportImport
import com.nielcode.kupass.data.repository.PasswordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Manages the password list, search, delete, and export/import functionality.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PasswordRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val passwords: StateFlow<List<PasswordEntity>>

    /** Result message for export/import operations */
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    init {
        val database = KupassDatabase.getInstance(application)
        val dao = database.passwordDao()
        repository = PasswordRepository(dao)

        passwords = _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllPasswords()
            } else {
                repository.searchPasswords(query)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.deletePassword(password)
        }
    }

    fun exportPasswords(uri: Uri) {
        viewModelScope.launch {
            try {
                val allPasswords = repository.getAllPasswords().first()
                if (allPasswords.isEmpty()) {
                    _operationMessage.value = "no_data"
                    return@launch
                }
                val json = JsonExportImport.exportToJson(allPasswords)
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                _operationMessage.value = "export_success"
            } catch (e: Exception) {
                _operationMessage.value = "export_failed"
            }
        }
    }

    fun importPasswords(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: return@launch

                val imported = JsonExportImport.importFromJson(json)
                imported.forEach { entity ->
                    // Insert as new entries (reset ID so Room auto-generates)
                    repository.insertPassword(entity.copy(id = 0))
                }
                _operationMessage.value = "import_success"
            } catch (e: Exception) {
                _operationMessage.value = "import_failed"
            }
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }
}
