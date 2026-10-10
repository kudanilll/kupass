package com.nielcode.kupass.ui.screens.home

import com.nielcode.kupass.data.local.db.PasswordEntity
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** Presentation identity only; never used for persistence or import deduplication. */
internal data class VaultGroupKey(val namespace: Namespace, val value: String) {
    enum class Namespace {
        WebHost,
        AndroidPackage,
        SiteName,
    }

    val lazyKey: String
        get() = "group:${namespace.name}:$value"
}

internal data class VaultGroup(val key: VaultGroupKey, val accounts: List<PasswordEntity>)

/** Groups every account without changing entities, IDs, or their stored fields. */
internal fun groupVault(accounts: List<PasswordEntity>): List<VaultGroup> =
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
            val authority =
                url.substringAfter("://")
                    .substringBefore('/')
                    .substringBefore('?')
                    .substringBefore('#')
            val packageName = authority.substringAfterLast('@', "")
            packageName.takeIf(androidPackagePattern::matches)?.let {
                VaultGroupKey(VaultGroupKey.Namespace.AndroidPackage, it)
            }
        } else {
            webHost(url)?.let {
                VaultGroupKey(VaultGroupKey.Namespace.WebHost, it)
            }
        }
    return primaryKey
        ?: VaultGroupKey(
            VaultGroupKey.Namespace.SiteName,
            account.siteName.trim().lowercase(Locale.ROOT),
        )
}

private val androidPackagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

/** URI parsing is local: private hosts, localhost and IP literals never need DNS or icon lookup. */
private fun webHost(url: String): String? {
    if (url.isEmpty()) return null
    return try {
        val uri =
            URI(
                if (url.startsWith("//")) "https:$url"
                else if (url.contains("://")) url else "https://$url"
            )
        val nonWebUri =
            uri.scheme.lowercase(Locale.ROOT) !in listOf("http", "https") || uri.port > MAX_PORT
        // Without a web scheme, a colon may denote a port, not a different URI scheme.
        val opaqueScheme =
            !url.contains("://") && uri.rawUserInfo != null && URI(url).scheme != null
        if (nonWebUri || opaqueScheme) {
            null
        } else {
            uri.host?.lowercase(Locale.ROOT)?.trimEnd('.')?.removePrefix("www.")?.takeIf {
                it.isNotEmpty()
            }
        }
    } catch (_: URISyntaxException) {
        null
    }
}

private const val MAX_PORT = 65535
