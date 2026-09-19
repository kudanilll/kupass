package com.nielcode.kupass.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.di.appContainer
import com.nielcode.kupass.ui.screens.VaultEvent
import com.nielcode.kupass.ui.screens.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the Home tab: the decrypted vault list, search, and delete. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: PasswordRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val passwords: StateFlow<List<PasswordEntity>>

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
                    started = WhileUiSubscribed,
                    initialValue = emptyList(),
                )
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch { repository.deletePassword(password) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(appContainer().passwordRepository) }
        }
    }
}
