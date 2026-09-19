package com.nielcode.kupass.testing

import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [PasswordDao] that mirrors the Room queries closely enough for unit tests. */
class FakePasswordDao : PasswordDao {
    private val rows = MutableStateFlow<List<PasswordEntity>>(emptyList())
    private var nextId = 1L

    /** Raw stored rows (encrypted fields as written by the repository). */
    val stored: List<PasswordEntity>
        get() = rows.value

    override fun getAll(): Flow<List<PasswordEntity>> = rows

    override suspend fun getAllOnce(): List<PasswordEntity> = rows.value

    override fun getById(id: Long): Flow<PasswordEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun insert(password: PasswordEntity): Long {
        val id = if (password.id == 0L) nextId++ else password.id
        rows.value = rows.value.filterNot { it.id == id } + password.copy(id = id)
        return id
    }

    override suspend fun insertAll(passwords: List<PasswordEntity>): List<Long> = passwords.map { insert(it) }

    override suspend fun update(password: PasswordEntity) {
        rows.value = rows.value.map { if (it.id == password.id) password else it }
    }

    override suspend fun updateAll(passwords: List<PasswordEntity>) {
        val byId = passwords.associateBy { it.id }
        rows.value = rows.value.map { byId[it.id] ?: it }
    }

    override suspend fun delete(password: PasswordEntity) = deleteById(password.id)

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
