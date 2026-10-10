package com.nielcode.kupass.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GooglePasswordCsvTest {
    @Test
    fun `official header maps all fields preserving Android facets and exact whitespace`() {
        val facet = "android://certificate_hash=@com.example.app/"
        val csv = "name,url,username,password,note\r\nApp,$facet, me , \t , recovery \r\n"

        val result = GooglePasswordCsv.decode(csv)

        val entry = result.entries.single()
        assertEquals("App", entry.siteName)
        assertEquals(facet, entry.url)
        assertEquals(" me ", entry.username)
        assertEquals(" \t ", entry.password)
        assertEquals(" recovery ", entry.notes)
        assertEquals(0, result.skipped)
        assertEquals(0L, entry.id)
    }

    @Test
    fun `BOM reordered quoted headers and older four columns work`() {
        val csv = "\uFEFF\"password\",username,url,name\n padded ,user,https://example.com,Example"

        val entry = GooglePasswordCsv.decode(csv).entries.single()

        assertEquals(" padded ", entry.password)
        assertEquals("Example", entry.siteName)
        assertEquals("", entry.notes)
    }

    @Test
    fun `quoted commas multiline text escaped quotes and trailing empty columns work`() {
        val csv =
            HEADER +
                "\r\n\"Site, one\",https://example.com,\"user\"\"name\",\"a\r\nb\nc\",\r\n" +
                "Site2,https://example.org,,p,\"note,\"\"quoted\"\"\nnext\""

        val entries = GooglePasswordCsv.decode(csv).entries

        assertEquals(2, entries.size)
        assertEquals("Site, one", entries[0].siteName)
        assertEquals("user\"name", entries[0].username)
        assertEquals("a\r\nb\nc", entries[0].password)
        assertEquals("", entries[0].notes)
        assertEquals("", entries[1].username)
        assertEquals("note,\"quoted\"\nnext", entries[1].notes)
    }

    @Test
    fun `missing values and invalid URLs are skipped but empty username and note are valid`() {
        val result =
            GooglePasswordCsv.decode(
                HEADER +
                    "\n" +
                    ",https://example.com,u,p,\n" +
                    "No password,https://example.com,u,,\n" +
                    "Bad URL,not a url,u,p,\n" +
                    "Missing URL,,u,p,\n" +
                    "Bad app,android://broken,u,p,\n" +
                    "Valid,https://example.com,,p,"
            )

        assertEquals(5, result.skipped)
        assertEquals("Valid", result.entries.single().siteName)
    }

    @Test
    fun `malformed quotes separators and widths reject the whole file`() {
        val valid = HEADER + "\nGood,https://example.com,u,p,\n"
        listOf(
                "Bad,https://example.com,u,\"unterminated,",
                "Bad,https://example.com,u,un\"quoted,",
                "Bad,https://example.com,u,\"closed\"suffix,",
                "Bad,https://example.com,u,p",
                "Bad,https://example.com,u,p,,extra",
            )
            .forEach { row ->
                assertThrows(BackupException.InvalidCsv::class.java) {
                    GooglePasswordCsv.decode(valid + row)
                }
            }
    }

    @Test
    fun `missing duplicate or unknown headers are rejected`() {
        listOf(
                "",
                "name,url,username",
                "name,url,password,password",
                "name,url,username,pass",
                "name,url,username,password,note,notes",
                "name,url,username,password,note,extra",
            )
            .forEach { csv ->
                assertThrows(BackupException.InvalidCsv::class.java) {
                    GooglePasswordCsv.decode(csv)
                }
            }
        assertEquals(emptyList<Any>(), GooglePasswordCsv.decode(HEADER).entries)
    }

    @Test
    fun `header spelling variants and blank lines do not change credentials`() {
        val result =
            GooglePasswordCsv.decode(
                "\uFEFF\r\n\" Name \", URL , Username , Password , Notes\r\n\r\n" +
                    "Example,https://example.com, me , \t , notes \r\n\r\n"
            )
        val account = result.entries.single()
        assertEquals(" me ", account.username)
        assertEquals(" \t ", account.password)
        assertEquals(" notes ", account.notes)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `semicolon and CR record separators preserve quoted delimiters and line breaks`() {
        val result =
            GooglePasswordCsv.decode(
                "Name;Url;Username;Password;Notes\r" +
                    "Example;https://example.com;user;\"x; y\r\nz\";\r\r"
            )
        assertEquals("x; y\r\nz", result.entries.single().password)
        assertEquals("", result.entries.single().notes)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `data row cap includes skipped rows but not header or quoted newlines`() {
        val atLimit = HEADER + "\n" + "Missing,,u,,\n".repeat(BackupCodec.MAX_ENTRIES)
        assertEquals(BackupCodec.MAX_ENTRIES, GooglePasswordCsv.decode(atLimit).skipped)
        assertEquals(BackupCodec.MAX_ENTRIES, GooglePasswordCsv.decode(atLimit + "\n").skipped)
        assertThrows(BackupException.TooLarge::class.java) {
            GooglePasswordCsv.decode(atLimit + "Missing,,u,,\n")
        }
        val multiline =
            HEADER +
                "\nSite,https://example.com,u,\"" +
                "x\n".repeat(BackupCodec.MAX_ENTRIES + 1) +
                "\","
        assertEquals(1, GooglePasswordCsv.decode(multiline).entries.size)
    }

    private companion object {
        const val HEADER = "name,url,username,password,note"
    }
}
