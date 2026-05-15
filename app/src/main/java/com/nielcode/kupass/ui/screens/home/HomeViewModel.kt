package com.nielcode.kupass.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Home screen.
 * Manages the password list and search functionality with reactive Flow from Room.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PasswordRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val passwords: StateFlow<List<PasswordEntity>>

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
}
