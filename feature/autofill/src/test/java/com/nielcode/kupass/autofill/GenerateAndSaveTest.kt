package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.testing.FakePasswordDao
import java.io.IOException
import javax.crypto.KeyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GenerateAndSaveTest {

    private val dao = FakePasswordDao()
    private lateinit var repository: PasswordRepository
    private val target = AutofillTarget.App("com.example.shop")
    private val form =
        ParsedForm(
            packageName = "com.example.shop",
            webDomain = null,
            usernameField = 0,
            passwordFields = listOf(1, 2),
            isSignUp = true,
            usernameValue = "dan",
            passwordValue = null,
        )

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        repository = PasswordRepository(dao, cryptoDispatcher = UnconfinedTestDispatcher())
    }

    @Test
    fun `password is returned only after the insert`() = runTest {
        val password = repository.generateAndSave(false, target, form, "Shop") { _, _ -> "Gen-1" }

        assertEquals("Gen-1", password)
        val saved = repository.getAllPasswords().first().single()
        assertEquals("Gen-1", saved.password)
        assertEquals("dan", saved.username)
        assertEquals("Shop", saved.siteName)
        assertEquals("android://com.example.shop", saved.url)
    }

    @Test
    fun `insert failure returns null`() = runTest {
        val failing =
            object : PasswordDao by FakePasswordDao() {
                override suspend fun insert(password: PasswordEntity): Long =
                    throw IOException("disk")
            }
        val broken = PasswordRepository(failing, cryptoDispatcher = UnconfinedTestDispatcher())

        assertNull(broken.generateAndSave(false, target, form, "Shop") { _, _ -> "Gen-1" })
    }

    @Test
    fun `lookalike setting reaches the generator`() = runTest {
        var seen: Pair<Int, Boolean>? = null
        repository.generateAndSave(true, target, form, "Shop") { length, exclude ->
            seen = length to exclude
            "Gen-1"
        }
        assertEquals(16 to true, seen)
    }

    @Test
    fun `choosing twice creates two entries`() = runTest {
        repository.generateAndSave(false, target, form, "Shop") { _, _ -> "Gen-1" }
        repository.generateAndSave(false, target, form, "Shop") { _, _ -> "Gen-2" }

        val saved = repository.getAllPasswords().first()
        assertEquals(setOf("Gen-1", "Gen-2"), saved.map { it.password }.toSet())
        assertEquals(2, saved.size)
    }
}
