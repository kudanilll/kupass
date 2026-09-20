package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.CryptoManager

/*
 * Field-level mapping between stored rows (every text field encrypted) and plaintext entities,
 * plus the in-memory search, ordering, and duplicate identity used once rows are decrypted.
 */

internal fun PasswordEntity.decrypted() =
    copy(
        siteName = CryptoManager.decrypt(siteName),
        username = CryptoManager.decrypt(username),
        password = CryptoManager.decrypt(password),
        url = CryptoManager.decrypt(url),
        notes = CryptoManager.decrypt(notes),
    )

internal fun PasswordEntity.encrypted() =
    copy(
        siteName = CryptoManager.encrypt(siteName),
        username = CryptoManager.encrypt(username),
        password = CryptoManager.encrypt(password),
        url = CryptoManager.encrypt(url),
        notes = CryptoManager.encrypt(notes),
    )

internal fun PasswordEntity.isCurrentFormat() =
    listOf(siteName, username, password, url, notes).all(CryptoManager::isCurrentFormat)

internal fun PasswordEntity.identity() =
    listOf(siteName.trim().lowercase(), username, url, password)

internal fun PasswordEntity.matches(needle: String) =
    needle.isEmpty() ||
        listOf(siteName, username, url, notes).any { it.contains(needle, ignoreCase = true) }

internal fun List<PasswordEntity>.sortedForDisplay() =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.siteName })

/** Entries matching [query] on site, username, URL, or notes (case-insensitive); all if blank. */
fun List<PasswordEntity>.filterByQuery(query: String): List<PasswordEntity> {
    val needle = query.trim()
    return if (needle.isEmpty()) this else filter { it.matches(needle) }
}
