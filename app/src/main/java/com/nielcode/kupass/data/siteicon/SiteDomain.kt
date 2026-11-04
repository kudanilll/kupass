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

/** `android://<cert hash>@<package>/`, the app facet in Google Password Manager exports. */
private val AndroidFacet =
    Regex("^android://[^@/]+@([a-z0-9_]+(?:\\.[a-z0-9_]+)+)/?$", RegexOption.IGNORE_CASE)

/** Package prefixes that are real top-level domains; anything else (`android.*`) isn't guessed. */
private val PackageTopLevelDomains = setOf("com", "org", "net", "io", "co", "app", "dev", "me")

/** Second-level labels of country domains, so `id.co.bri.brimo` becomes `bri.co.id`. */
private val CountrySecondLevels =
    setOf("co", "com", "net", "org", "or", "ac", "go", "gov", "edu", "web", "my", "sch")

/**
 * The public domain to look up a site icon for, or null when there is none worth sending.
 *
 * Uses the entry's URL, falling back to the site name when it looks like a domain. A single-word
 * public site name such as `GitHub` is conservatively treated as `github.com`. Only the bare
 * hostname is returned (lowercase, no `www.`, no path, port, or credentials). IP addresses and
 * private names are rejected so nothing internal leaves the device.
 *
 * App entries (`android://` facets) never send their package name. They use the site name when it
 * is a name such as `Instagram`, otherwise a domain read from the package (`com.instagram.android`
 * becomes `instagram.com`). Malformed facets get no icon.
 */
fun siteDomainOf(url: String, siteName: String): String? {
    val trimmedUrl = url.trim()
    return if (trimmedUrl.startsWith("android://", ignoreCase = true)) {
        AndroidFacet.matchEntire(trimmedUrl)?.groupValues?.get(1)?.let { appDomainOf(it, siteName) }
    } else {
        hostOf(url) ?: hostOf(siteName) ?: guessedDotComDomain(siteName)
    }
}

/** An app entry's domain: from its site name, unless that is just the package name. */
private fun appDomainOf(packageName: String, siteName: String): String? {
    val name = siteName.trim().takeUnless { it.equals(packageName, ignoreCase = true) }.orEmpty()
    return hostOf(name) ?: guessedDotComDomain(name) ?: domainOfPackage(packageName)
}

/** `com.instagram.android` -> `instagram.com`, `id.co.bri.brimo` -> `bri.co.id`. */
private fun domainOfPackage(packageName: String): String? {
    val labels = packageName.lowercase(Locale.ROOT).split('.')
    val topLevel = labels.first()
    if (topLevel !in PackageTopLevelDomains && topLevel.length != 2) return null
    val parts =
        if (labels.size > 2 && topLevel.length == 2 && labels[1] in CountrySecondLevels) 3 else 2
    return hostOf(labels.take(parts).reversed().joinToString("."))
}

private fun guessedDotComDomain(siteName: String): String? {
    val name = siteName.trim().lowercase(Locale.ROOT)
    return name.takeIf { PublicSiteName.matches(it) && it !in PrivateSuffixes }?.let { "$it.com" }
}

private fun hostOf(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.startsWith("android://", ignoreCase = true)) return null
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
