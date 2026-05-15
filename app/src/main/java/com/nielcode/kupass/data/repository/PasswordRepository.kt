package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for password data access.
 * Acts as a single source of truth, abstracting the DAO from ViewModels.
 */
class PasswordRepository(private val passwordDao: PasswordDao) {

    fun getAllPasswords(): Flow<List<PasswordEntity>> = passwordDao.getAll()

    fun searchPasswords(query: String): Flow<List<PasswordEntity>> = passwordDao.search(query)

    fun getPasswordById(id: Long): Flow<PasswordEntity?> = passwordDao.getById(id)

    suspend fun insertPassword(password: PasswordEntity): Long = passwordDao.insert(password)

    suspend fun updatePassword(password: PasswordEntity) = passwordDao.update(password)

    suspend fun deletePassword(password: PasswordEntity) = passwordDao.delete(password)

    suspend fun deletePasswordById(id: Long) = passwordDao.deleteById(id)
}
