package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.utils.CryptoManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for password data access. Acts as a single source of truth, abstracting the DAO from
 * ViewModels.
 *
 * Encryption is transparent here: callers only ever see plaintext entities. All crypto runs on
 * [cryptoDispatcher], never on the main thread. Read flows fail with
 * [com.nielcode.kupass.utils.CryptoException] if stored data can't be decrypted.
 */
class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private fun PasswordEntity.decrypt() = copy(password = CryptoManager.decrypt(password))

    private fun PasswordEntity.encrypt() = copy(password = CryptoManager.encrypt(password))

    fun getAllPasswords(): Flow<List<PasswordEntity>> =
        passwordDao.getAll().map { list -> list.map { it.decrypt() } }.flowOn(cryptoDispatcher)

    fun searchPasswords(query: String): Flow<List<PasswordEntity>> =
        passwordDao.search(query).map { list -> list.map { it.decrypt() } }.flowOn(cryptoDispatcher)

    fun getPasswordById(id: Long): Flow<PasswordEntity?> =
        passwordDao.getById(id).map { it?.decrypt() }.flowOn(cryptoDispatcher)

    suspend fun insertPassword(password: PasswordEntity): Long =
        passwordDao.insert(withContext(cryptoDispatcher) { password.encrypt() })

    suspend fun updatePassword(password: PasswordEntity) =
        passwordDao.update(withContext(cryptoDispatcher) { password.encrypt() })

    suspend fun deletePassword(password: PasswordEntity) = passwordDao.deleteById(password.id)

    suspend fun deletePasswordById(id: Long) = passwordDao.deleteById(id)
}
