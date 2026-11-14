package com.nielcode.kupass.update

import com.nielcode.kupass.data.local.prefs.PreferenceManager

/** What a look for a newer release found. */
internal sealed interface CheckResult {
    data class Found(val update: AvailableUpdate) : CheckResult

    data object UpToDate : CheckResult

    /** The request failed, and no release found earlier is newer than this build. */
    data object Failed : CheckResult

    /** Debug builds never check. */
    data object Unavailable : CheckResult
}

/**
 * Looks for a newer release. GitHub is asked at most once an hour unless the user asks in Settings.
 * The releases newer than this build are remembered, so one found earlier is offered at every app
 * open until it is installed, offline too.
 *
 * @param installed the running build; null for debug builds, which never check.
 * @param fetchReleases blocking request for the release list; null when it failed.
 * @param now wall-clock time in milliseconds.
 */
internal class UpdateChecker(
    private val prefs: PreferenceManager,
    private val installed: AppVersion?,
    private val fetchReleases: () -> List<GitHubRelease>?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Blocking when it asks GitHub. [force] asks even when the last answer is recent. */
    fun check(force: Boolean = false): CheckResult {
        val installed = installed ?: return CheckResult.Unavailable
        val time = now()
        // A clock set back counts as an hour gone.
        val recent = !force && time - prefs.updateCheckedAt in 0 until CHECK_INTERVAL_MILLIS
        val fetched = if (recent) null else fetchReleases()
        if (fetched != null) {
            prefs.updateReleases =
                encodeReleases(
                    fetched.filter { release ->
                        AppVersion.fromTag(release.tagName)?.let { it > installed } == true
                    }
                )
            prefs.updateCheckedAt = time
        }
        val releases = fetched ?: prefs.updateReleases?.let(::parseReleases).orEmpty()
        val update = newestUpdate(releases, installed)
        return when {
            update != null -> CheckResult.Found(update)
            recent || fetched != null -> CheckResult.UpToDate
            else -> CheckResult.Failed
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MILLIS = 60 * 60 * 1000L
    }
}
