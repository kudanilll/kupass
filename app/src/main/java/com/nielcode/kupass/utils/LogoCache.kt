package com.nielcode.kupass.utils

import android.util.LruCache

/**
 * Simple in-memory cache for logo URLs.
 * Key = site domain (mis: github.com), Value = final URL from Favget API.
 */
object LogoCache {
    // Max 100 entries
    private val cache = LruCache<String, String>(100)

    fun get(domain: String): String? = cache.get(domain)

    fun put(domain: String, url: String) {
        cache.put(domain, url)
    }
}
