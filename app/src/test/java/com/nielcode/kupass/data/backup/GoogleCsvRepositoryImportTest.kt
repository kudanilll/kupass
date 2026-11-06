package com.nielcode.kupass.data.backup

import com.nielcode.kupass.data.repository.ImportResult
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.testing.FakePasswordDao
import javax.crypto.KeyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Google CSV decoding (app) feeding the repository import (core:data). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoogleCsvRepositoryImportTest {

    private lateinit var dao: FakePasswordDao
    private lateinit var repository: PasswordRepository

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        dao = FakePasswordDao()
        repository = PasswordRepository(dao, cryptoDispatcher = UnconfinedTestDispatcher())
    }

    @Test
    fun `CSV import keeps distinct identities exact whitespace and every field encrypted`() =
        runTest {
            val csv =
                "name,url,username,password,note\n" +
                    "App,android://hash@com.example.app/,alice, \t ,recovery\n" +
                    "App,android://hash@com.example.app/,bob, \t ,other\n" +
                    "App,android://hash@com.example.app/,alice, padded ,note\n" +
                    "App,https://example.com,alice, \t ,website\n"
            val decoded = GooglePasswordCsv.decode(csv)
            val result = repository.importPasswords(decoded.entries + decoded.entries.first())

            assertEquals(ImportResult(imported = 4, skipped = 1), result)
            assertTrue(
                dao.stored.all { row ->
                    listOf(row.siteName, row.username, row.url, row.password, row.notes).all {
                        it.startsWith(CryptoManager.PREFIX_V2)
                    }
                }
            )
            assertEquals(
                decoded.entries.toSet(),
                repository.getAllPasswords().first().map { it.copy(id = 0) }.toSet(),
            )
            assertEquals(
                ImportResult(imported = 0, skipped = 4),
                repository.importPasswords(decoded.entries),
            )
        }
}
