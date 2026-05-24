package com.nielcode.kupass.ui.screens.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents the state of a save operation.
 */
sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data class Error(val message: String) : SaveState
}

/**
 * ViewModel for the Password Editor screen.
 * Handles creating new passwords and editing existing ones.
 */
class PasswordEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PasswordRepository

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /** The existing password being edited, null if creating new. */
    private val _existingPassword = MutableStateFlow<PasswordEntity?>(null)
    val existingPassword: StateFlow<PasswordEntity?> = _existingPassword.asStateFlow()

    /** Whether we are in edit mode (vs create mode). */
    val isEditMode: Boolean get() = _existingPassword.value != null

    init {
        val database = KupassDatabase.getInstance(application)
        val dao = database.passwordDao()
        repository = PasswordRepository(dao)
    }

    /**
     * Load an existing password for editing.
     * Call this when the editor is opened with a valid passwordId.
     */
    fun loadPassword(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            repository.getPasswordById(id)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null
                )
                .collect { entity ->
                    if (entity != null && _existingPassword.value == null) {
                        _existingPassword.value = entity
                    }
                }
        }
    }

    fun savePassword(
        siteName: String,
        username: String,
        password: String,
        url: String,
        notes: String
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val existing = _existingPassword.value
                if (existing != null) {
                    // Edit mode: update existing password
                    val updated = existing.copy(
                        siteName = siteName.trim(),
                        username = username.trim(),
                        password = password,
                        url = url.trim(),
                        notes = notes.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updatePassword(updated)
                } else {
                    // Create mode: insert new password
                    val entity = PasswordEntity(
                        siteName = siteName.trim(),
                        username = username.trim(),
                        password = password,
                        url = url.trim(),
                        notes = notes.trim()
                    )
                    repository.insertPassword(entity)
                }
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
