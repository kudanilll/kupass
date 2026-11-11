package com.nielcode.kupass.update

import com.nielcode.kupass.data.local.prefs.PreferenceManager
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Decides at app open whether to offer an update. The release list is requested at most once a day;
 * a release it finds is remembered and offered at every open until it is installed or postponed
 * with "Later".
 *
 * @param installed the running build; null for debug builds, which never check.
 * @param fetchReleases blocking request for the release list; null when it failed.
 */
internal class UpdateChecker(
    private val prefs: PreferenceManager,
    private val installed: AppVersion?,
    private val fetchReleases: () -> List<GitHubRelease>?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** The update to offer now, if any. Blocking: it may request the release list. */
    fun updateToOffer(): AvailableUpdate? {
        if (installed == null || !prefs.checkForUpdates) return null
        val time = now()
        // A clock set back gives a negative age, which also asks again.
        if (time - prefs.lastUpdateCheckMillis !in 0 until CHECK_INTERVAL_MILLIS) {
            prefs.lastUpdateCheckMillis = time
            fetchReleases()?.let { releases ->
                prefs.pendingUpdate =
                    newestUpdate(releases, installed)?.let { Json.encodeToString(it) }
            }
        }
        return remembered()?.takeIf { it.version > installed && it.tag != prefs.skippedUpdateTag }
    }

    /** "Later": this release is not offered again; a newer one will be. */
    fun skip(update: AvailableUpdate) {
        prefs.skippedUpdateTag = update.tag
    }

    private fun remembered(): AvailableUpdate? =
        try {
            prefs.pendingUpdate?.let { Json.decodeFromString<AvailableUpdate>(it) }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L
    }
}
