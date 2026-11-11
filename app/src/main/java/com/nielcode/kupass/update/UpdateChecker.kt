package com.nielcode.kupass.update

import com.nielcode.kupass.data.local.prefs.PreferenceManager

/**
 * Decides at app open whether to offer an update. Every open requests the release list, so a new
 * release shows up at the next open; nothing is offered when the request fails.
 *
 * @param installed the running build; null for debug builds, which never check.
 * @param fetchReleases blocking request for the release list; null when it failed.
 */
internal class UpdateChecker(
    private val prefs: PreferenceManager,
    private val installed: AppVersion?,
    private val fetchReleases: () -> List<GitHubRelease>?,
) {
    /** The update to offer now, if any. Blocking: it requests the release list. */
    fun updateToOffer(): AvailableUpdate? {
        if (installed == null || !prefs.checkForUpdates) return null
        return fetchReleases()?.let { newestUpdate(it, installed) }
    }
}
