package com.nielcode.kupass.ui.screens.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Handles saving new passwords to the Room database.
 */
class PasswordEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PasswordRepository

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        val database = KupassDatabase.getInstance(application)
        val dao = database.passwordDao()
        repository = PasswordRepository(dao)
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
                val entity = PasswordEntity(
                    siteName = siteName.trim(),
                    username = username.trim(),
                    password = password,
                    url = url.trim(),
                    notes = notes.trim()
                )
                repository.insertPassword(entity)
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
