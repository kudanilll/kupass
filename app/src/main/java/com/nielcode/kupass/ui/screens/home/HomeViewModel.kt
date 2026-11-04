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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

    val allPasswords: StateFlow<List<PasswordEntity>>

    /** Search-filtered passwords. */
    val passwords: StateFlow<List<PasswordEntity>>

    /** Vault grouped by site/app identity, computed off the main thread. */
    val groups: StateFlow<List<VaultGroup>>

    /** Groups filtered by search query, computed off the main thread. */
    val visibleGroups: StateFlow<List<VaultGroup>>

    private val _events = Channel<VaultEvent>(Channel.BUFFERED)

    /** One-shot results for the UI; each event is delivered once. */
    val events: Flow<VaultEvent> = _events.receiveAsFlow()

    init {
        // Decrypt the vault once per database change and keep it alive in memory.
        allPasswords =
            repository
                .getAllPasswords()
                .catch {
                    _events.trySend(VaultEvent.VaultReadFailed)
                    emit(emptyList())
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // Group on background thread so the main thread never runs URI parsing.
        groups =
            allPasswords
                .map { groupVault(it) }
                .flowOn(filterDispatcher)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // Filter on background thread.
        passwords =
            combine(allPasswords, _searchQuery) { entries, query ->
                    entries.filterByQuery(query)
                }
                .flowOn(filterDispatcher)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // Visible groups = groups whose accounts overlap with the filtered set.
        visibleGroups =
            combine(groups, passwords) { allGroups, matching ->
                    val ids = matching.mapTo(HashSet()) { it.id }
                    allGroups.filter { group -> group.accounts.any { it.id in ids } }
                }
                .flowOn(filterDispatcher)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
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
