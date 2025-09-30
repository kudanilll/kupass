package com.nielcode.kupass.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Represents a single account for a site, which can have multiple credentials..
 *
 * @property id Unique ID for this site account.
 * @property site Site name (e.g., "Google", "Facebook").
 * @property note Additional notes for this site.
 * @property credentials List of credentials (username/password) associated with this site.
 */
@Parcelize
data class SiteAccount(
    val id: Long,
    val site: String,
    val note: String?,
    val credentials: List<UserCredential>
) : Parcelable {
    /** Favget endpoint (domain required; fallback guesses "<site>.com"). */
    val logoUrl: String
        get() {
            val d = site.trim().lowercase().replace("\\s+".toRegex(), "") + ".com"
            return "https://favget-api.vercel.app/v1/icon?domain=$d"
        }
}

/**
 * Represents a set of credentials (username and password).
 *
 * @property username Username or email.
 * @property password Password.
 * @property lastUpdated The last date these credentials were changed.
 */
@Parcelize
data class UserCredential(
    val username: String,
    val password: String,
    val lastUpdated: Date
) : Parcelable