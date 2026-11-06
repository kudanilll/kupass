package com.nielcode.kupass.data

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

private const val ANDROID_SCHEME = "android://"
private const val MAX_PORT = 65535
private val androidPackagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

/**
 * The normalized web host of a stored URL or bare domain: lowercase, no trailing dot, no `www.`.
 * Only http(s) counts. URI parsing is local: private hosts, localhost and IP literals never need
 * DNS.
 */
fun webHostOf(url: String): String? {
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

/**
 * The package of an app entry: `android://<pkg>` (saved by Kupass) or Google's
 * `android://<cert>@<pkg>/` facet. The signing certificate part is ignored.
 */
fun androidPackageOf(url: String): String? {
    val trimmed = url.trim()
    if (!trimmed.startsWith(ANDROID_SCHEME, ignoreCase = true)) return null
    val authority =
        trimmed
            .substring(ANDROID_SCHEME.length)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
    return authority.substringAfterLast('@').takeIf(androidPackagePattern::matches)
}

/** The URL Kupass stores for an app entry. */
fun androidUrlOf(packageName: String): String = "$ANDROID_SCHEME$packageName"
