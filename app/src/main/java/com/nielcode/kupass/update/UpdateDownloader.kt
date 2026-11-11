package com.nielcode.kupass.update

import com.nielcode.kupass.data.siteicon.readAtMost
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Downloads an update's APK into [dir] (app cache) and checks it against the SHA-256 published with
 * the release. Only the verified file is returned; anything else is deleted. Android then still
 * refuses to install it unless it is signed with Kupass's own key.
 */
internal class UpdateDownloader(private val dir: File) {

    /**
     * Blocking, but stops at the next chunk when the coroutine is cancelled.
     *
     * @param onProgress whole percent, reported when it changes.
     * @return the verified APK, or null if anything failed.
     */
    suspend fun download(update: AvailableUpdate, onProgress: (Int) -> Unit = {}): File? {
        val requestable =
            isAllowedUrl(update.apkUrl) &&
                isAllowedUrl(update.checksumUrl) &&
                update.apkSize in 1..MAX_APK_BYTES
        val expected = if (requestable) publishedChecksum(update.checksumUrl) else null
        if (expected == null) return null

        dir.deleteRecursively()
        dir.mkdirs()
        val file = File(dir, FILE_NAME)
        var verified = false
        try {
            val digest = httpGet(update.apkUrl) { copy(it, file, update.apkSize, onProgress) }
            verified = digest == expected
        } finally {
            if (!verified) file.delete()
        }
        return file.takeIf { verified }
    }

    /** The first token of a `sha256sum` line: 64 hex digits, any case. */
    private fun publishedChecksum(url: String): String? =
        httpGet(url) { it.readAtMost(MAX_CHECKSUM_BYTES) }
            ?.decodeToString()
            ?.trim()
            ?.substringBefore(' ')
            ?.lowercase(Locale.ROOT)
            ?.takeIf { SHA256_HEX.matches(it) }

    /**
     * Copies [input] into [file], stopping once it is longer than [size]. Returns its SHA-256, or
     * null if the size differs.
     */
    private suspend fun copy(
        input: InputStream,
        file: File,
        size: Long,
        onProgress: (Int) -> Unit,
    ): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val progress = PercentReporter(size, onProgress)
        var total = 0L
        var read = 0
        file.outputStream().use { output ->
            while (read >= 0 && total <= size) {
                currentCoroutineContext().ensureActive()
                read = input.read(buffer)
                if (read > 0) {
                    total += read
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    progress.report(total)
                }
            }
        }
        currentCoroutineContext().ensureActive()
        return if (total == size) {
            digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
        } else {
            null
        }
    }

    /** Reports whole percent of [size], only when it changes. */
    private class PercentReporter(private val size: Long, private val onProgress: (Int) -> Unit) {
        private var last = -1

        fun report(bytes: Long) {
            val percent = (bytes.coerceAtMost(size) * PERCENT / size).toInt()
            if (percent != last) {
                last = percent
                onProgress(percent)
            }
        }
    }

    private companion object {
        const val FILE_NAME = "kupass-update.apk"
        const val MAX_APK_BYTES = 100L * 1024 * 1024
        const val MAX_CHECKSUM_BYTES = 4 * 1024
        const val PERCENT = 100
        val SHA256_HEX = Regex("[0-9a-f]{64}")
    }
}
