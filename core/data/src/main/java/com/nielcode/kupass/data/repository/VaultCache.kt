package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.cache.DecryptedFields
import com.nielcode.kupass.data.local.cache.VaultSnapshotStore
import com.nielcode.kupass.data.local.db.PasswordEntity

/**
 * Decrypted fields per stored row, so each row goes through the Keystore once instead of on every
 * vault change and every app start.
 *
 * Rows are keyed by a fingerprint of their ciphertext. Every encryption uses a fresh IV, so equal
 * fingerprints always decrypt to the same fields and any edit produces a new fingerprint: the
 * database stays the only source of truth. [decrypt] keeps exactly the rows it was given, so
 * deleted entries leave memory and the on-disk snapshot. Fields of rows still being written wait in
 * `pending` until the database returns them.
 */
internal class VaultCache(private val store: VaultSnapshotStore?) {
    private val lock = Any()
    private val saveLock = Any()
    private var live: Map<String, DecryptedFields>? = null
    private val pending = HashMap<String, DecryptedFields>()
    @Volatile private var saved: Map<String, DecryptedFields>? = null

    /** [rows] with plaintext fields, decrypting only the rows this cache hasn't seen. */
    fun decrypt(rows: List<PasswordEntity>): List<PasswordEntity> =
        synchronized(lock) {
            val known = live ?: store?.load().orEmpty().also { saved = it }
            val next = HashMap<String, DecryptedFields>(rows.size)
            val plain = rows.map { row ->
                val fingerprint = row.fingerprint()
                val fields = known[fingerprint] ?: pending[fingerprint] ?: row.decryptFields()
                next[fingerprint] = fields
                row.withFields(fields)
            }
            pending.keys.removeAll(next.keys)
            // Keep the same instance when nothing changed, so persist() can skip the write.
            live = if (next.keys == known.keys) known else next
            plain
        }

    /** Remembers the plaintext of rows about to be written, so their next read skips decryption. */
    fun remember(stored: List<PasswordEntity>, plain: List<PasswordEntity>) =
        synchronized(lock) {
            stored.zip(plain).forEach { (row, entry) ->
                pending[row.fingerprint()] = entry.fields()
            }
        }

    /** Drops remembered rows whose write failed. */
    fun forget(stored: List<PasswordEntity>) =
        synchronized(lock) { stored.forEach { pending.remove(it.fingerprint()) } }

    /** Writes the current vault to the snapshot if it changed since the last write. */
    fun persist() {
        val store = store ?: return
        synchronized(saveLock) {
            val current = synchronized(lock) { live } ?: return
            if (current !== saved && store.save(current)) saved = current
        }
    }
}
