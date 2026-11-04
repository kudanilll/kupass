package com.nielcode.kupass.data.backup

import com.nielcode.kupass.data.local.db.PasswordEntity
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** Google/Chromium and standard password manager CSV exports, parsed locally. */
object GooglePasswordCsv {
    private val androidFacet =
        Regex("^android://[A-Za-z0-9_=-]+@[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+/?$")

    class Result(val entries: List<PasswordEntity>, val skipped: Int)

    /** Validates the entire file before returning any entries for insertion. */
    fun decode(content: String): Result {
        val reader = csvReader(content)
        val header = readHeader(reader)
        val columns = header.withIndex().associate { it.value to it.index }
        val entries = mutableListOf<PasswordEntity>()
        var rows = 0
        var skipped = 0
        while (true) {
            val row = reader.nextRow() ?: break
            if (++rows > BackupCodec.MAX_ENTRIES) throw BackupException.TooLarge()
            val entity = parseRow(row, header.size, columns)
            if (entity != null) entries += entity else skipped++
        }
        return Result(entries, skipped)
    }

    private fun parseRow(
        row: List<String>,
        headerSize: Int,
        columns: Map<String, Int>,
    ): PasswordEntity? {
        // Pad short rows; for long rows just read by index (extra columns are ignored).
        val cells = if (row.size < headerSize) row + List(headerSize - row.size) { "" } else row
        val rawName = columns["name"]?.let { cells.getOrNull(it) }?.trim()
        val rawUrl = columns["url"]?.let { cells.getOrNull(it) }?.trim().orEmpty()
        val name = rawName ?: rawUrl
        val url = if (rawUrl.isBlank()) "" else rawUrl
        if (name.isBlank() || !validUrl(url)) return null
        return PasswordEntity(
            siteName = name,
            url = url,
            username = columns["username"]?.let { cells.getOrNull(it) }.orEmpty(),
            password = columns["password"]?.let { cells.getOrNull(it) }.orEmpty(),
            notes = columns["note"]?.let { cells.getOrNull(it) }.orEmpty(),
        )
    }

    private fun readHeader(reader: CsvReader): List<String> {
        val rawHeader = reader.nextRow() ?: throw BackupException.InvalidCsv()
        val header = rawHeader.map(::canonicalColumnName)
        if (!isValidHeader(header)) throw BackupException.InvalidCsv()
        return header
    }

    private fun isValidHeader(header: List<String>): Boolean {
        if (header.isEmpty()) return false
        val known = header.filter { it in setOf("name", "url", "username", "password", "note") }
        val names = known.toSet()
        val hasIdentity =
            "name" in names ||
                "url" in names ||
                "password" in names ||
                "username" in names ||
                "note" in names
        return known.size == names.size && hasIdentity
    }

    private val nameAliases =
        setOf(
            "name",
            "title",
            "site",
            "sitename",
            "app",
            "application",
            "nama",
            "namasitus",
            "judul",
            "situs",
        )
    private val urlAliases =
        setOf(
            "url",
            "uri",
            "website",
            "link",
            "web",
            "host",
            "domain",
            "tautan",
            "alamat",
            "alamatweb",
            "alamatsitus",
        )
    private val userAliases =
        setOf(
            "username",
            "user",
            "login",
            "email",
            "account",
            "surel",
            "pengguna",
            "namapengguna",
            "id",
            "userid",
            "akun",
        )
    private val passAliases =
        setOf("password", "pass", "pwd", "secret", "sandi", "katasandi", "pin", "kuncisandi")
    private val noteAliases =
        setOf(
            "note",
            "notes",
            "comment",
            "comments",
            "keterangan",
            "deskripsi",
            "description",
            "catatan",
            "memo",
        )

    private fun canonicalColumnName(raw: String): String {
        val clean =
            raw.trim()
                .trim('"', '\'', '`')
                .trim()
                .lowercase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
        return when (clean) {
            in nameAliases -> "name"
            in urlAliases -> "url"
            in userAliases -> "username"
            in passAliases -> "password"
            in noteAliases -> "note"
            else -> clean
        }
    }

    private fun csvReader(content: String): CsvReader {
        val text = content.removePrefix("\uFEFF").trimStart('\r', '\n')
        val delimiter = detectDelimiter(text)
        return CsvReader(text, delimiter)
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.substringBefore('\n').substringBefore('\r')
        var inQuotes = false
        var commas = 0
        var semicolons = 0
        var tabs = 0
        for (ch in firstLine) {
            if (ch == '"') inQuotes = !inQuotes
            else if (!inQuotes) {
                when (ch) {
                    ',' -> commas++
                    ';' -> semicolons++
                    '\t' -> tabs++
                }
            }
        }
        val counts = listOf(',' to commas, ';' to semicolons, '\t' to tabs)
        return counts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first ?: ','
    }

    private fun validUrl(url: String): Boolean =
        if (url.isBlank()) true
        else if (url.startsWith("android://", ignoreCase = true)) androidFacet.matches(url)
        else
            try {
                val withScheme = if ("://" in url) url else "https://$url"
                val uri = URI(withScheme)
                (uri.scheme.equals("http", ignoreCase = true) ||
                    uri.scheme.equals("https", ignoreCase = true)) && !uri.host.isNullOrBlank()
            } catch (_: URISyntaxException) {
                false
            }
}

/**
 * Quoted fields preserve every character; only record separators and header names are normalized.
 */
private class CsvReader(private val content: String, private val delimiter: Char) {
    private var position = 0

    fun nextRow(): List<String>? {
        var row: List<String>?
        do {
            row = readRow()
        } while (row?.let { it.size == 1 && it.single().isBlank() } == true)
        return row
    }

    private fun readRow(): List<String>? {
        if (position == content.length) return null
        val row = mutableListOf<String>()
        var ended: Boolean
        do {
            row += field()
            ended = position == content.length
            if (!ended) {
                val separator = content[position++]
                if (separator == '\r') {
                    if (position < content.length && content[position] == '\n') position++
                    ended = true
                } else if (separator == '\n') {
                    ended = true
                } else if (separator != delimiter) {
                    throw BackupException.InvalidCsv()
                }
            }
        } while (!ended)
        return row
    }

    private fun field(): String {
        val value = StringBuilder()
        if (position < content.length && content[position] == '"') {
            position++
            while (position < content.length) {
                val char = content[position++]
                if (char != '"') value.append(char)
                else if (position < content.length && content[position] == '"') {
                    position++
                    value.append('"')
                } else return value.toString()
            }
            throw BackupException.InvalidCsv()
        }
        while (
            position < content.length &&
                content[position] != delimiter &&
                content[position] !in "\r\n"
        ) {
            val char = content[position++]
            if (char == '"') throw BackupException.InvalidCsv()
            value.append(char)
        }
        return value.toString()
    }
}
