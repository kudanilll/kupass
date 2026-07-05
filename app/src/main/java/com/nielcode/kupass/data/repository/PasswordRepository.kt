package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.utils.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for password data access. Acts as a single source of truth, abstracting the DAO from
 * ViewModels.
 */
class PasswordRepository(private val passwordDao: PasswordDao) {

    // transparent encryption/decryption at repository layer
    // avoid touching UI components or DAO interfaces

    private fun PasswordEntity.decrypt() = copy(password = CryptoManager.decrypt(password))

    private fun PasswordEntity.encrypt() = copy(password = CryptoManager.encrypt(password))

    fun getAllPasswords(): Flow<List<PasswordEntity>> =
        passwordDao.getAll().map { list -> list.map { it.decrypt() } }

    fun searchPasswords(query: String): Flow<List<PasswordEntity>> =
        passwordDao.search(query).map { list -> list.map { it.decrypt() } }

    fun getPasswordById(id: Long): Flow<PasswordEntity?> =
        passwordDao.getById(id).map { it?.decrypt() }

    suspend fun insertPassword(password: PasswordEntity): Long =
        passwordDao.insert(password.encrypt())

    suspend fun updatePassword(password: PasswordEntity) = passwordDao.update(password.encrypt())

    suspend fun deletePassword(password: PasswordEntity) = passwordDao.delete(password.encrypt())

    suspend fun deletePasswordById(id: Long) = passwordDao.deleteById(id)
}
