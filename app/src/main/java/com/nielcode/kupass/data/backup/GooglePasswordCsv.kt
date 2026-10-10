package com.nielcode.kupass.data.backup

import com.nielcode.kupass.data.local.db.PasswordEntity
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

private const val MAX_COLUMNS = 5

/** Google/Chromium password exports, parsed locally without normalizing credential fields. */
object GooglePasswordCsv {
    private val requiredHeader = setOf("name", "url", "username", "password")
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
            if (row.size != header.size) throw BackupException.InvalidCsv()
            val name = row[columns.getValue("name")]
            val url = row[columns.getValue("url")]
            val password = row[columns.getValue("password")]
            if (name.isBlank() || password.isEmpty() || !validUrl(url)) {
                skipped++
            } else {
                entries +=
                    PasswordEntity(
                        siteName = name,
                        url = url,
                        username = row[columns.getValue("username")],
                        password = password,
                        notes = columns["note"]?.let { row[it] }.orEmpty(),
                    )
            }
        }
        return Result(entries, skipped)
    }

    private fun readHeader(reader: CsvReader): List<String> {
        val header =
            (reader.nextRow() ?: throw BackupException.InvalidCsv()).map {
                it.trim().lowercase(Locale.ROOT).let { name ->
                    if (name == "notes") "note" else name
                }
            }
        val names = header.toSet()
        if (
            names !in listOf(requiredHeader, requiredHeader + "note") || names.size != header.size
        ) {
            throw BackupException.InvalidCsv()
        }
        return header
    }

    private fun csvReader(content: String): CsvReader {
        val text = content.removePrefix("\uFEFF").trimStart('\r', '\n')
        val firstLine = text.substringBefore('\n').substringBefore('\r')
        val delimiter = if (';' in firstLine && ',' !in firstLine) ';' else ','
        return CsvReader(text, delimiter)
    }

    private fun validUrl(url: String): Boolean =
        if (url.startsWith("android://")) androidFacet.matches(url)
        else
            try {
                val uri = URI(url)
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
            // A valid Google row has at most five columns. Bound malformed delimiter floods too.
            if (row.size > MAX_COLUMNS) throw BackupException.InvalidCsv()
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
