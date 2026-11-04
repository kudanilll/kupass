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
    fun `missing values and invalid URLs are skipped but empty username and password are valid`() {
        val result =
            GooglePasswordCsv.decode(
                HEADER +
                    "\n" +
                    ",https://example.com,u,p,\n" +
                    "Bad URL,not a url,u,p,\n" +
                    "Bad app,android://broken,u,p,\n" +
                    "EmptyPass,https://example.com,u,,\n" +
                    "Valid,https://example.com,,p,"
            )

        assertEquals(3, result.skipped)
        assertEquals(2, result.entries.size)
        assertEquals("EmptyPass", result.entries[0].siteName)
        assertEquals("", result.entries[0].password)
        assertEquals("Valid", result.entries[1].siteName)
    }

    @Test
    fun `CSV with missing password column or missing username column or extra columns and aliases work`() {
        val noPass = "name,url,username\n" + "NoPassCol,https://example.com,alice\n"
        val noPassResult = GooglePasswordCsv.decode(noPass)
        assertEquals(0, noPassResult.skipped)
        assertEquals("NoPassCol", noPassResult.entries.single().siteName)
        assertEquals("", noPassResult.entries.single().password)
        assertEquals("alice", noPassResult.entries.single().username)

        val aliasesAndExtra =
            "title,website,login,pass,notes,extra_col,folder\n" +
                "MyBank,https://mybank.com,john,secret123,my note,ignored1,Personal\n"
        val aliasResult = GooglePasswordCsv.decode(aliasesAndExtra)
        assertEquals(0, aliasResult.skipped)
        val entry = aliasResult.entries.single()
        assertEquals("MyBank", entry.siteName)
        assertEquals("https://mybank.com", entry.url)
        assertEquals("john", entry.username)
        assertEquals("secret123", entry.password)
        assertEquals("my note", entry.notes)
    }

    @Test
    fun `indonesian google export and large column count work`() {
        val indo =
            "Nama,URL,Nama Pengguna,Kata Sandi,Catatan\n" +
                "Tokopedia,https://tokopedia.com,budi,rahasia123,akun utama\n"
        val indoResult = GooglePasswordCsv.decode(indo)
        assertEquals(0, indoResult.skipped)
        val entry = indoResult.entries.single()
        assertEquals("Tokopedia", entry.siteName)
        assertEquals("https://tokopedia.com", entry.url)
        assertEquals("budi", entry.username)
        assertEquals("rahasia123", entry.password)
        assertEquals("akun utama", entry.notes)

        // 100 columns (unlimited)
        val extraCols = (1..95).joinToString(",") { "col$it" }
        val bigHeader = "name,url,username,password,note,$extraCols\n"
        val extraVals = (1..95).joinToString(",") { "val$it" }
        val bigRow = "BigSite,https://example.com,user,pass,note,$extraVals\n"
        val bigResult = GooglePasswordCsv.decode(bigHeader + bigRow)
        assertEquals(0, bigResult.skipped)
        assertEquals("BigSite", bigResult.entries.single().siteName)
    }

    @Test
    fun `rows with missing note column or empty url and username are imported successfully`() {
        val csv =
            HEADER +
                "\n" +
                "OnlyCreds,https://example.com,user,pass\n" +
                "EmptyNote,https://example.com,user,pass,\n" +
                "NoUrl, ,user,pass\n" +
                "NoUsername,https://example.com,,pass\n" +
                "BareMinimal,,,pass\n"
        val result = GooglePasswordCsv.decode(csv)
        assertEquals(0, result.skipped)
        assertEquals(5, result.entries.size)

        assertEquals("OnlyCreds", result.entries[0].siteName)
        assertEquals("https://example.com", result.entries[0].url)
        assertEquals("", result.entries[0].notes)

        assertEquals("EmptyNote", result.entries[1].siteName)
        assertEquals("", result.entries[1].notes)

        assertEquals("NoUrl", result.entries[2].siteName)
        assertEquals("", result.entries[2].url)
        assertEquals("", result.entries[2].notes)

        assertEquals("NoUsername", result.entries[3].siteName)
        assertEquals("", result.entries[3].username)

        assertEquals("BareMinimal", result.entries[4].siteName)
        assertEquals("", result.entries[4].url)
        assertEquals("", result.entries[4].username)
        assertEquals("", result.entries[4].notes)
    }

    @Test
    fun `malformed quotes reject the whole file but extra columns are tolerated`() {
        val valid = HEADER + "\nGood,https://example.com,u,p,\n"
        listOf(
                "Bad,https://example.com,u,\"unterminated,",
                "Bad,https://example.com,u,un\"quoted,",
                "Bad,https://example.com,u,\"closed\"suffix,",
            )
            .forEach { row ->
                assertThrows(BackupException.InvalidCsv::class.java) {
                    GooglePasswordCsv.decode(valid + row)
                }
            }
        // Extra columns in a data row are silently ignored, not rejected.
        val extra = GooglePasswordCsv.decode(valid + "Site,https://example.org,u,p,n,extra1,extra2")
        assertEquals(2, extra.entries.size)
        assertEquals("Site", extra.entries[1].siteName)
    }

    @Test
    fun `unrelated or empty headers are rejected`() {
        listOf(
                "",
                "unrelated,headers,here",
                "price,quantity,item",
                "name,url,password,password",
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
        val atLimit = HEADER + "\n" + ",,u,,\n".repeat(BackupCodec.MAX_ENTRIES)
        assertEquals(BackupCodec.MAX_ENTRIES, GooglePasswordCsv.decode(atLimit).skipped)
        assertEquals(BackupCodec.MAX_ENTRIES, GooglePasswordCsv.decode(atLimit + "\n").skipped)
        assertThrows(BackupException.TooLarge::class.java) {
            GooglePasswordCsv.decode(atLimit + ",,u,,\n")
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
