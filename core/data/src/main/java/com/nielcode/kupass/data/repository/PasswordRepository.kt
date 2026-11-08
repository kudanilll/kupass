package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.cache.VaultSnapshotStore
import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.CryptoException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
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
 *
 * Decrypted rows are cached ([VaultCache]), so only new or changed rows are decrypted, and an
 * encrypted snapshot in [snapshotStore] lets the next app start skip decryption entirely.
 */
class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,
    snapshotStore: VaultSnapshotStore? = null,
) {
    private val cache = VaultCache(snapshotStore)

    fun getAllPasswords(): Flow<List<PasswordEntity>> =
        passwordDao
            .getAll()
            .transform { rows ->
                emit(cache.decrypt(rows).sortedForDisplay())
                // After emitting, so writing the snapshot never delays the list.
                cache.persist()
            }
            .flowOn(cryptoDispatcher)

    /** Case-insensitive match on site name, username, URL, and notes. */
    fun searchPasswords(query: String): Flow<List<PasswordEntity>> =
        getAllPasswords().map { it.filterByQuery(query) }.flowOn(cryptoDispatcher)

    fun getPasswordById(id: Long): Flow<PasswordEntity?> =
        // Not cached on purpose: a single row stays fail-closed whenever the Keystore can't
        // decrypt.
        passwordDao.getById(id).map { it?.decrypted() }.flowOn(cryptoDispatcher)

    suspend fun insertPassword(password: PasswordEntity): Long {
        val encrypted = withContext(cryptoDispatcher) { password.encrypted() }
        return storing(listOf(encrypted), listOf(password)) { passwordDao.insert(encrypted) }
    }

    suspend fun updatePassword(password: PasswordEntity) {
        val encrypted = withContext(cryptoDispatcher) { password.encrypted() }
        storing(listOf(encrypted), listOf(password)) { passwordDao.update(encrypted) }
    }

    /**
     * Adds backup entries to the vault atomically. Entries without a site name, and duplicates of
     * an existing entry or of an earlier entry in the same backup, are skipped. A duplicate has the
     * same site name (ignoring case and surrounding spaces), username, URL, and password.
     */
    suspend fun importPasswords(
        entries: List<PasswordEntity>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): ImportResult {
        val existing = getAllPasswords().first()
        return withContext(cryptoDispatcher) {
            val seen = existing.mapTo(HashSet()) { it.identity() }
            val toInsert = entries.filter {
                it.siteName.isNotBlank() && seen.add(it.identity())
            }
            val total = toInsert.size
            if (total > 0) onProgress?.invoke(0, total)
            // Encrypt everything before touching the database so a crypto failure inserts nothing.
            val encrypted = toInsert.mapIndexed { index, entry ->
                val enc = entry.copy(id = 0).encrypted()
                onProgress?.invoke(index + 1, total)
                enc
            }
            currentCoroutineContext().ensureActive()
            if (encrypted.isNotEmpty()) {
                storing(encrypted, toInsert) { passwordDao.insertAll(encrypted) }
            }
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
            val rewritten =
                passwordDao
                    .getAllOnce()
                    .filterNot { it.isCurrentFormat() }
                    .mapNotNull { row ->
                        try {
                            row.decrypted().let { plain -> plain.encrypted() to plain }
                        } catch (_: CryptoException) {
                            null
                        }
                    }
            val upgraded = rewritten.map { it.first }
            if (upgraded.isNotEmpty()) {
                storing(upgraded, rewritten.map { it.second }) { passwordDao.updateAll(upgraded) }
            }
            upgraded.size
        }

    /**
     * Runs [write] for [encrypted] rows, remembering their [plain] fields so the next read of those
     * rows skips decryption. If the write fails, nothing is remembered.
     */
    private suspend fun <T> storing(
        encrypted: List<PasswordEntity>,
        plain: List<PasswordEntity>,
        write: suspend () -> T,
    ): T {
        cache.remember(encrypted, plain)
        var stored = false
        try {
            return write().also { stored = true }
        } finally {
            if (!stored) cache.forget(encrypted)
        }
    }
}

/** Outcome of [PasswordRepository.importPasswords]. */
data class ImportResult(val imported: Int, val skipped: Int)
