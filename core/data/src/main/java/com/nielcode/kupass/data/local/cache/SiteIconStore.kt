package com.nielcode.kupass.data.local.cache

import java.io.File
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** An icon as kept between launches: PNG bytes and when they were fetched (epoch millis). */
class StoredIcon(val png: ByteArray, val fetchedAt: Long)

/**
 * Site icons kept between launches, so each icon is fetched from Favget once instead of on every
 * app start. It is a [SealedFile] like the vault cache, so the file doesn't reveal which sites are
 * saved. Icons older than 30 days are dropped on load and fetched again.
 */
class SiteIconStore(file: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val sealed = SealedFile(file, "kupass-site-icons-v1")

    /** Icons by domain, without expired ones; empty when missing or unreadable. */
    fun load(): Map<String, StoredIcon> {
        val text = sealed.read() ?: return emptyMap()
        val icons = readableOrNull {
            Json.decodeFromString<Snapshot>(text).icons.mapValues { (_, icon) ->
                StoredIcon(Base64.getDecoder().decode(icon.png), icon.fetchedAt)
            }
        }
        if (icons == null) sealed.delete()
        val oldest = clock() - MAX_AGE_MILLIS
        return icons.orEmpty().filterValues { it.fetchedAt >= oldest }
    }

    /** Replaces the stored icons. Returns false if they couldn't be written. */
    fun save(icons: Map<String, StoredIcon>): Boolean {
        val entries = icons.mapValues { (_, icon) ->
            Entry(Base64.getEncoder().encodeToString(icon.png), icon.fetchedAt)
        }
        return sealed.write(Json.encodeToString(Snapshot(entries)))
    }

    @Serializable private class Entry(val png: String, val fetchedAt: Long)

    @Serializable private class Snapshot(val icons: Map<String, Entry>)

    private companion object {
        const val MAX_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
