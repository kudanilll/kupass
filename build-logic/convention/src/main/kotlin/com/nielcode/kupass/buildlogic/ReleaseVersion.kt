package com.nielcode.kupass.buildlogic

/** What a release tag sets in the build: the `versionCode`, and the beta number (0 for stable). */
data class ReleaseVersion(val versionCode: Int, val beta: Int)

/**
 * Reads a release tag, `vX.Y.Z` for a stable release or `vX.Y.Z-beta.N` for a beta, that must match
 * [versionName].
 *
 * versionCode = X·1 000 000 + Y·10 000 + Z·100 + N, with 99 in place of N for the stable release.
 * Each beta then updates to the next one, and every beta of X.Y.Z updates to X.Y.Z itself.
 *
 * @throws IllegalArgumentException if the tag is malformed, names another version, or doesn't fit
 *   the formula: Y and Z up to 99, N from 1 to 98, and Google Play's versionCode limit.
 */
fun releaseVersion(tag: String, versionName: String): ReleaseVersion {
    val match =
        requireNotNull(RELEASE_TAG.matchEntire(tag)) {
            "Release tag '$tag' is not vX.Y.Z or vX.Y.Z-beta.N"
        }
    val (version, betaText) = match.destructured
    require(version == versionName) { "Release tag $tag does not match versionName $versionName" }
    val (major, minor, patch) = version.split('.').map(String::toInt)
    val beta = betaText.toIntOrNull() ?: 0
    require(minor <= MAX_PART && patch <= MAX_PART) {
        "Release tag $tag: minor and patch go up to $MAX_PART"
    }
    require(beta <= MAX_BETA) { "Release tag $tag: beta numbers go up to $MAX_BETA" }
    val versionCode =
        major.toLong() * 1_000_000 + minor * 10_000 + patch * 100 + if (beta == 0) STABLE else beta
    require(versionCode <= MAX_VERSION_CODE) {
        "Release tag $tag gives versionCode $versionCode, above $MAX_VERSION_CODE"
    }
    return ReleaseVersion(versionCode.toInt(), beta)
}

/** SemVer core without leading zeros, plus an optional `-beta.N` with N from 1 to 99. */
private val RELEASE_TAG =
    Regex("""v((?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*))(?:-beta\.([1-9]\d?))?""")

private const val MAX_PART = 99

/** 99 is the stable release, so it is the one beta number a tag can't use. */
private const val STABLE = 99
private const val MAX_BETA = STABLE - 1

/** The largest versionCode Google Play accepts. */
private const val MAX_VERSION_CODE = 2_100_000_000L
