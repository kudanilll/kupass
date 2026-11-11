package com.nielcode.kupass.update

import kotlinx.serialization.Serializable

/**
 * A Kupass release: X.Y.Z plus the beta number N, which is 0 for a stable release. Versions sort in
 * release order, each stable release after its betas, the same order as the versionCodes the
 * release build derives from tags (build-logic `ReleaseVersion.kt`).
 */
@Serializable
data class AppVersion(val major: Int, val minor: Int, val patch: Int, val beta: Int = 0) :
    Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }, { it.betaRank })

    override fun toString(): String = "$major.$minor.$patch" + if (beta > 0) "-beta.$beta" else ""

    private val betaRank: Int
        get() = if (beta == 0) Int.MAX_VALUE else beta

    companion object {
        /** `v3.1.0` or `v3.1.0-beta.2`; null for any other tag. */
        fun fromTag(tag: String): AppVersion? =
            TAG.matchEntire(tag)?.destructured?.let { (version, beta) ->
                parse(version, beta.toIntOrNull() ?: 0)
            }

        /** The running build, from `BuildConfig.VERSION_NAME` and `BuildConfig.BETA_NUMBER`. */
        fun installed(versionName: String, beta: Int): AppVersion? =
            VERSION.matchEntire(versionName)?.let { parse(versionName, beta) }

        private fun parse(version: String, beta: Int): AppVersion? {
            val (major, minor, patch) = version.split('.').map { it.toIntOrNull() ?: return null }
            return AppVersion(major, minor, patch, beta).takeIf { beta <= MAX_BETA }
        }

        private const val CORE = """(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)"""
        private val VERSION = Regex(CORE)
        private val TAG = Regex("""v($CORE)(?:-beta\.([1-9]\d?))?""")

        /** The release workflow refuses beta 99: that versionCode belongs to the stable release. */
        private const val MAX_BETA = 98
    }
}
