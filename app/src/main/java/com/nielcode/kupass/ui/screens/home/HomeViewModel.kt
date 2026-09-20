package com.nielcode.kupass.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.data.repository.filterByQuery
import com.nielcode.kupass.di.appContainer
import com.nielcode.kupass.ui.screens.VaultEvent
import com.nielcode.kupass.ui.screens.WhileUiSubscribed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the Home tab: the decrypted vault list, search, and delete. */
class HomeViewModel(
    private val repository: PasswordRepository,
    filterDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val passwords: StateFlow<List<PasswordEntity>>

    private val _events = Channel<VaultEvent>(Channel.BUFFERED)

    /** One-shot results for the UI; each event is delivered once. */
    val events: Flow<VaultEvent> = _events.receiveAsFlow()

    init {
        // Decrypt the vault once per database change and keep it while the UI is subscribed...
        val vault =
            repository
                .getAllPasswords()
                // Never crash or show ciphertext when the vault can't be decrypted.
                .catch {
                    _events.trySend(VaultEvent.VaultReadFailed)
                    emit(emptyList())
                }
                .stateIn(viewModelScope, WhileUiSubscribed, emptyList())
        // ...so each search keystroke only filters the in-memory list instead of decrypting again.
        passwords =
            combine(vault, _searchQuery) { entries, query -> entries.filterByQuery(query) }
                .flowOn(filterDispatcher)
                .stateIn(viewModelScope, WhileUiSubscribed, emptyList())
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
