package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.utils.CryptoException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for password data access. Acts as a single source of truth, abstracting the DAO from
 * ViewModels.
 *
 * Every text field (`siteName`, `username`, `password`, `url`, `notes`) is encrypted at rest and
 * transparently decrypted here; callers only ever see plaintext entities. Because the database
 * holds ciphertext, sorting and search happen in memory after decryption (decision Q1). All crypto
 * runs on [cryptoDispatcher], never on the main thread. Read flows fail with [CryptoException] if
 * stored data can't be decrypted.
 */
class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    fun getAllPasswords(): Flow<List<PasswordEntity>> =
        passwordDao
            .getAll()
            .map { rows -> rows.map { it.decrypted() }.sortedForDisplay() }
            .flowOn(cryptoDispatcher)

    /** Case-insensitive match on site name, username, URL, and notes. */
    fun searchPasswords(query: String): Flow<List<PasswordEntity>> {
        val needle = query.trim()
        return getAllPasswords()
            .map { list -> list.filter { it.matches(needle) } }
            .flowOn(cryptoDispatcher)
    }

    fun getPasswordById(id: Long): Flow<PasswordEntity?> =
        passwordDao.getById(id).map { it?.decrypted() }.flowOn(cryptoDispatcher)

    suspend fun insertPassword(password: PasswordEntity): Long =
        passwordDao.insert(withContext(cryptoDispatcher) { password.encrypted() })

    suspend fun updatePassword(password: PasswordEntity) =
        passwordDao.update(withContext(cryptoDispatcher) { password.encrypted() })

    /**
     * Adds backup entries to the vault atomically. Entries without a site name or password, and
     * duplicates of an existing entry or of an earlier entry in the same backup, are skipped. A
     * duplicate has the same site name (ignoring case and surrounding spaces), username, URL, and
     * password.
     */
    suspend fun importPasswords(entries: List<PasswordEntity>): ImportResult {
        val existing = getAllPasswords().first()
        return withContext(cryptoDispatcher) {
            val seen = existing.mapTo(HashSet()) { it.identity() }
            val toInsert = entries.filter {
                it.siteName.isNotBlank() && it.password.isNotBlank() && seen.add(it.identity())
            }
            // Encrypt everything before touching the database so a crypto failure inserts nothing.
            val encrypted = toInsert.map { it.copy(id = 0).encrypted() }
            if (encrypted.isNotEmpty()) passwordDao.insertAll(encrypted)
            ImportResult(imported = encrypted.size, skipped = entries.size - encrypted.size)
        }
    }

    suspend fun deletePassword(password: PasswordEntity) = passwordDao.deleteById(password.id)

    suspend fun deletePasswordById(id: Long) = passwordDao.deleteById(id)

    /**
     * Re-encrypts rows still stored in a legacy format (plaintext fields from before full-field
     * encryption, or v1 ciphertext) into the current format. Idempotent and cheap when nothing is
     * left to upgrade. Rows that can't be decrypted are left untouched rather than overwritten.
     *
     * @return the number of rows upgraded.
     */
    suspend fun upgradeStoredFormat(): Int =
        withContext(cryptoDispatcher) {
            val upgraded =
                passwordDao
                    .getAllOnce()
                    .filterNot { it.isCurrentFormat() }
                    .mapNotNull { row ->
                        try {
                            row.decrypted().encrypted()
                        } catch (_: CryptoException) {
                            null
                        }
                    }
            if (upgraded.isNotEmpty()) passwordDao.updateAll(upgraded)
            upgraded.size
        }
}

/** Outcome of [PasswordRepository.importPasswords]. */
data class ImportResult(val imported: Int, val skipped: Int)
