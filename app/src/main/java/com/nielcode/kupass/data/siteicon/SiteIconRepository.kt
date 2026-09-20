package com.nielcode.kupass.data.siteicon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.reflect.KMutableProperty0
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Site icons as the UI sees them: a synchronous cache peek and a suspending load. */
interface SiteIcons {
    /** The icon if it is already in memory, so rows don't flash their fallback while scrolling. */
    fun peek(domain: String): ImageBitmap?

    /** The icon for [domain], or null when there is none (or the feature is off). */
    suspend fun load(domain: String): ImageBitmap?
}

/**
 * Opt-in site icons (roadmap FEAT-2), off by default.
 *
 * Icons are cached in memory only. A disk cache would reveal which sites are in the vault even
 * though the vault itself is encrypted. Concurrent requests for a domain share one fetch, and
 * misses and failures are remembered for a while so scrolling doesn't hammer the API.
 *
 * @param source null when this build has no Favget API key; the feature is then unavailable.
 * @param enabledSetting where the opt-in choice is stored.
 */
class SiteIconRepository(
    private val source: IconSource?,
    private val enabledSetting: KMutableProperty0<Boolean>,
    private val scope: CoroutineScope,
    private val decode: (ByteArray) -> ImageBitmap? = ::decodeSiteIcon,
    private val ioDispatcher: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(MAX_CONCURRENT_FETCHES),
    private val clock: () -> Long = System::currentTimeMillis,
) : SiteIcons {

    /** Whether this build can fetch icons at all. */
    val isAvailable: Boolean
        get() = source != null

    private val _enabled = MutableStateFlow(enabledSetting.get() && source != null)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val icons =
        object : LruCache<String, ImageBitmap>(MEMORY_CACHE_BYTES) {
            override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
        }
    private val retryAfter = HashMap<String, Long>()
    private val inFlight = HashMap<String, Deferred<ImageBitmap?>>()
    private val lock = Any()

    fun setEnabled(enabled: Boolean) {
        if (source == null || enabled == _enabled.value) return
        enabledSetting.set(enabled)
        _enabled.value = enabled
        if (!enabled) clear()
    }

    override fun peek(domain: String): ImageBitmap? =
        if (_enabled.value) icons.get(domain) else null

    override suspend fun load(domain: String): ImageBitmap? {
        val iconSource = source?.takeIf { _enabled.value } ?: return null
        return icons.get(domain)
            ?: pendingFetch(iconSource, domain)?.run {
                start()
                await()
            }
    }

    /** The shared fetch for [domain], or null while a recent miss or failure is remembered. */
    private fun pendingFetch(iconSource: IconSource, domain: String): Deferred<ImageBitmap?>? =
        synchronized(lock) {
            if ((retryAfter[domain] ?: 0L) > clock()) null
            else
                inFlight.getOrPut(domain) {
                    // Lazy, so it is registered before it can complete and unregister itself.
                    scope.async(ioDispatcher, start = CoroutineStart.LAZY) {
                        fetchAndCache(iconSource, domain)
                    }
                }
        }

    private fun fetchAndCache(iconSource: IconSource, domain: String): ImageBitmap? {
        val result = iconSource.fetch(domain)
        val icon = (result as? IconFetchResult.Found)?.bytes?.let(decode)
        synchronized(lock) {
            inFlight.remove(domain)
            // Turned off while fetching: keep nothing.
            if (!_enabled.value) return null
            when {
                icon != null -> icons.put(domain, icon)
                result == IconFetchResult.Failed ->
                    retryAfter[domain] = clock() + FAILURE_RETRY_MILLIS
                else -> retryAfter[domain] = clock() + MISS_RETRY_MILLIS
            }
        }
        return icon
    }

    private fun clear() {
        synchronized(lock) {
            inFlight.values.forEach { it.cancel() }
            inFlight.clear()
            retryAfter.clear()
            icons.evictAll()
        }
    }

    private companion object {
        const val MAX_CONCURRENT_FETCHES = 4
        const val MEMORY_CACHE_BYTES = 4 * 1024 * 1024
        const val FAILURE_RETRY_MILLIS = 5 * 60 * 1000L
        const val MISS_RETRY_MILLIS = 6 * 60 * 60 * 1000L
    }
}

/** Longest side of a decoded icon; rows show them at 28dp, so this covers xxxhdpi. */
private const val ICON_SIZE_PX = 96

/** Decodes and downsizes an icon. Returns null for anything that isn't a decodable image. */
fun decodeSiteIcon(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val decoded =
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
        else {
            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight))
                }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }
    return decoded?.let(::downscale)?.asImageBitmap()
}

/** Largest power of two that still decodes at least [ICON_SIZE_PX] on the longest side. */
private fun sampleSizeFor(longestSide: Int): Int {
    var sampleSize = 1
    while (longestSide / (sampleSize * 2) >= ICON_SIZE_PX) sampleSize *= 2
    return sampleSize
}

private fun downscale(bitmap: Bitmap): Bitmap {
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= ICON_SIZE_PX) return bitmap
    val scale = ICON_SIZE_PX.toFloat() / longest
    return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        .also { if (it !== bitmap) bitmap.recycle() }
}
