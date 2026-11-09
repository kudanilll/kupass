package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the vault list shows: [Loading] until the vault has been read once. */
sealed interface VaultUiState {
    data object Loading : VaultUiState

    /** [all] entries, the ones [matching] the search, and the groups to show for them. */
    data class Ready(
        val all: List<PasswordEntity>,
        val matching: List<PasswordEntity>,
        val visibleGroups: List<VaultGroup>,
    ) : VaultUiState
}

/** ViewModel for the Vault tab: the decrypted vault list, search, and delete. */
class HomeViewModel(
    private val repository: PasswordRepository,
    filterDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    /**
     * The search field's text and cursor, edited by the field itself. A `String` handed back to a
     * value-based field inside the lazy list reached it late and put the cursor back at the start.
     */
    val searchState = TextFieldState()

    private val _events = Channel<VaultEvent>(Channel.BUFFERED)

    /** One-shot results for the UI; each event is delivered once. */
    val events: Flow<VaultEvent> = _events.receiveAsFlow()

    /** The decrypted vault and its groups, computed together off the main thread per change. */
    private val vault: SharedFlow<GroupedVault> =
        repository
            .getAllPasswords()
            .map { entries -> GroupedVault(entries, groupVault(entries)) }
            .flowOn(filterDispatcher)
            .catch {
                _events.trySend(VaultEvent.VaultReadFailed)
                emit(GroupedVault(emptyList(), emptyList()))
            }
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    /** Every site/app group, for the account page. */
    val groups: StateFlow<List<VaultGroup>> =
        vault.map { it.groups }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The list state for the Vault tab, filtered by [searchState] off the main thread. */
    val uiState: StateFlow<VaultUiState> =
        combine(vault, snapshotFlow { searchState.text.toString() }) { (entries, groups), query ->
                val matching = entries.filterByQuery(query)
                val ids = matching.mapTo(HashSet()) { it.id }
                VaultUiState.Ready(
                    all = entries,
                    matching = matching,
                    visibleGroups = groups.filter { group -> group.accounts.any { it.id in ids } },
                )
            }
            .flowOn(filterDispatcher)
            .stateIn(viewModelScope, SharingStarted.Eagerly, VaultUiState.Loading)

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch { repository.deletePassword(password) }
    }

    private data class GroupedVault(val entries: List<PasswordEntity>, val groups: List<VaultGroup>)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(appContainer().passwordRepository) }
        }
    }
}
