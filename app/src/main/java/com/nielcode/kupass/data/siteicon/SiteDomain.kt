package com.nielcode.kupass.data.siteicon

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

private val PublicHostname =
    Regex("^(?=.{4,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z][a-z0-9-]{1,62}$")
private val PublicSiteName = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")

/** Suffixes that never resolve publicly; asking Favget about them would only leak names. */
private val PrivateSuffixes =
    listOf("localhost", "local", "internal", "lan", "home.arpa", "intranet", "corp", "invalid")

/**
 * The public domain to look up a site icon for, or null when there is none worth sending.
 *
 * Uses the entry's URL, falling back to the site name when it looks like a domain. A single-word
 * public site name such as `GitHub` is conservatively treated as `github.com`. Only the bare
 * hostname is returned (lowercase, no `www.`, no path, port, or credentials). IP addresses and
 * private names are rejected so nothing internal leaves the device.
 */
fun siteDomainOf(url: String, siteName: String): String? =
    hostOf(url) ?: hostOf(siteName) ?: guessedDotComDomain(siteName)

private fun guessedDotComDomain(siteName: String): String? {
    val name = siteName.trim().lowercase(Locale.ROOT)
    return name.takeIf { PublicSiteName.matches(it) && it !in PrivateSuffixes }?.let { "$it.com" }
}

private fun hostOf(value: String): String? {
    val trimmed = value.trim()
    val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
    val host =
        try {
            URI(withScheme).host
        } catch (_: URISyntaxException) {
            null
        }
    return host?.lowercase(Locale.ROOT)?.trimEnd('.')?.removePrefix("www.")?.takeIf { domain ->
        PublicHostname.matches(domain) && PrivateSuffixes.none { domain.endsWith(".$it") }
    }
}
