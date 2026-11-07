package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.webHostOf

/**
 * What a screen asks credentials for: an installed app, or a website shown by a trusted browser.
 */
sealed interface AutofillTarget {
    data class App(val packageName: String) : AutofillTarget

    data class Web(val host: String) : AutofillTarget
}

/**
 * Browsers whose reported web domain is trusted. Any other app could put any domain in a WebView,
 * so its domain is ignored. Shortcut: matched by package name only, not signing certificate; a
 * sideloaded app reusing one of these names is a known residual risk (docs/codebase/CONCERNS.md).
 */
internal val BROWSER_PACKAGES: Set<String> =
    setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.vivaldi.browser",
        "com.duckduckgo.mobile.android",
    )

object TargetResolver {
    /** `null` for Kupass itself and for a browser's own UI (no page domain). */
    fun resolve(packageName: String, webDomain: String?, ownPackage: String): AutofillTarget? =
        when (packageName) {
            ownPackage -> null
            in BROWSER_PACKAGES -> webDomain?.let(::webHostOf)?.let(AutofillTarget::Web)
            else -> AutofillTarget.App(packageName)
        }
}
