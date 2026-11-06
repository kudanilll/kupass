package com.nielcode.kupass.ui.screens.home

import com.nielcode.kupass.data.androidPackageOf
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.webHostOf
import java.util.Locale

/** Presentation identity only; never used for persistence or import deduplication. */
data class VaultGroupKey(val namespace: Namespace, val value: String) {
    enum class Namespace {
        WebHost,
        AndroidPackage,
        SiteName,
    }

    val lazyKey: String
        get() = "group:${namespace.name}:$value"
}

data class VaultGroup(val key: VaultGroupKey, val accounts: List<PasswordEntity>)

/** Groups every account without changing entities, IDs, or their stored fields. */
fun groupVault(accounts: List<PasswordEntity>): List<VaultGroup> =
    accounts
        .groupBy(::vaultGroupKey)
        .map { (key, entries) ->
            VaultGroup(
                key,
                entries.sortedWith(
                    compareBy<PasswordEntity> { it.username.lowercase(Locale.ROOT) }
                        .thenBy { it.siteName.lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                ),
            )
        }
        .sortedWith(
            compareBy<VaultGroup> { it.key.value.lowercase(Locale.ROOT) }
                .thenBy { it.key.value }
                .thenBy { it.key.namespace.name }
        )

internal fun vaultGroupKey(account: PasswordEntity): VaultGroupKey {
    val url = account.url.trim()
    val primaryKey =
        if (url.startsWith("android://", ignoreCase = true)) {
            androidPackageOf(url)?.let { VaultGroupKey(VaultGroupKey.Namespace.AndroidPackage, it) }
        } else {
            webHostOf(url)?.let { VaultGroupKey(VaultGroupKey.Namespace.WebHost, it) }
        }
    return primaryKey
        ?: VaultGroupKey(
            VaultGroupKey.Namespace.SiteName,
            account.siteName.trim().lowercase(Locale.ROOT),
        )
}
