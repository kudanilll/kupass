package com.nielcode.kupass.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.di.appContainer
import com.nielcode.kupass.ui.screens.recoverable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Represents the state of a save operation. */
sealed interface SaveState {
    data object Idle : SaveState

    data object Saving : SaveState

    data object Success : SaveState

    data object Error : SaveState
}

/**
 * ViewModel for the Password Editor screen. Handles creating new passwords and editing existing
 * ones.
 */
class PasswordEditorViewModel(
    private val repository: PasswordRepository,
    private var requestedId: Long = -1L,
) : ViewModel() {

    private var loadJob: Job? = null
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /** The existing password being edited, null if creating new. */
    private val _existingPassword = MutableStateFlow<PasswordEntity?>(null)
    val existingPassword: StateFlow<PasswordEntity?> = _existingPassword.asStateFlow()

    /** Whether we are in edit mode (vs create mode). */
    val isEditMode: Boolean
        get() = requestedId > 0

    /**
     * Load an existing password for editing. Call this when the editor is opened with a valid
     * passwordId.
     */
    fun loadPassword(id: Long) {
        if (id <= 0) return
        if (requestedId == id && loadJob != null) return
        requestedId = id
        loadJob?.cancel()
        _existingPassword.value = null
        _loadFailed.value = false
        loadJob = viewModelScope.launch {
            val entity = recoverable { repository.getPasswordById(id).first() }
            _existingPassword.value = entity
            _loadFailed.value = entity == null
        }
    }

    fun savePassword(
        siteName: String,
        username: String,
        password: String,
        url: String,
        notes: String,
    ) {
        if (requestedId > 0 && _existingPassword.value?.id != requestedId) {
            _saveState.value = SaveState.Error
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val saved = recoverable {
                val existing = _existingPassword.value
                if (existing != null) {
                    // Edit mode: update existing password
                    val updated =
                        existing.copy(
                            siteName = siteName.trim(),
                            username = username.trim(),
                            password = password,
                            url = url.trim(),
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis(),
                        )
                    repository.updatePassword(updated)
                } else {
                    // Create mode: insert new password
                    val entity =
                        PasswordEntity(
                            siteName = siteName.trim(),
                            username = username.trim(),
                            password = password,
                            url = url.trim(),
                            notes = notes.trim(),
                        )
                    repository.insertPassword(entity)
                }
            }
            _saveState.value = if (saved != null) SaveState.Success else SaveState.Error
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    /** Stop loading and drop decrypted state when the authorization gate removes this screen. */
    fun clearSensitiveState() {
        loadJob?.cancel()
        loadJob = null
        _existingPassword.value = null
        _loadFailed.value = false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PasswordEditorViewModel(appContainer().passwordRepository) }
        }

        fun factory(passwordId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { PasswordEditorViewModel(appContainer().passwordRepository, passwordId) }
        }
    }
}
