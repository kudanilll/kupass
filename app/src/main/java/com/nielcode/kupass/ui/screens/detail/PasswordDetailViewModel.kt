package com.nielcode.kupass.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.di.appContainer
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents the state of a delete operation.
 */
sealed interface DeleteState {
    data object Idle : DeleteState
    data object Deleting : DeleteState
    data object Success : DeleteState
    data class Error(val message: String) : DeleteState
}

/**
 * ViewModel for the Password Detail screen.
 * Loads a single password by ID and handles deletion.
 */
class PasswordDetailViewModel(private val repository: PasswordRepository) : ViewModel() {

    private var _passwordId: Long = -1L

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    // Will be initialized when loadPassword is called
    private val _password = MutableStateFlow<PasswordEntity?>(null)
    val password: StateFlow<PasswordEntity?> = _password.asStateFlow()

    fun loadPassword(id: Long) {
        _passwordId = id
        viewModelScope.launch {
            repository.getPasswordById(id)
                .catch { emit(null) } // undecryptable entry: show nothing rather than ciphertext
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null
                )
                .collect { _password.value = it }
        }
    }

    fun deletePassword() {
        val currentPassword = _password.value ?: return
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            try {
                repository.deletePassword(currentPassword)
                _deleteState.value = DeleteState.Success
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PasswordDetailViewModel(appContainer().passwordRepository) }
        }
    }
}
