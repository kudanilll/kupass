package com.nielcode.kupass.ui.screens.data

import android.content.ContentResolver
import android.database.SQLException
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.nielcode.kupass.data.backup.BackupCodec
import com.nielcode.kupass.data.local.db.PasswordDao
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.CryptoException
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.testing.FakePasswordDao
import com.nielcode.kupass.testing.MainDispatcherRule
import com.nielcode.kupass.ui.screens.VaultEvent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.crypto.KeyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private lateinit var dao: FakePasswordDao
    private lateinit var resolver: ContentResolver
    private lateinit var repository: PasswordRepository

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        dao = FakePasswordDao()
        resolver = RuntimeEnvironment.getApplication().contentResolver
        repository = PasswordRepository(dao, mainDispatcher.dispatcher)
    }

    @Test
    fun `CSV under a JSON name imports directly and closes the stream with combined skipped counts`() =
        runTest {
            val input =
                registerInput(CSV + "\nSite,https://example.com,u, padded ,note\nMissing,,u,,")
            val vm = viewModel()

            vm.importPasswords(INPUT)
            assertTrue(vm.backupBusy.value)
            vm.importPasswords(INPUT)
            advanceUntilIdle()

            assertEquals(VaultEvent.ImportSucceeded(imported = 1, skipped = 2), vm.events.first())
            assertTrue(input.closed)
            assertFalse(vm.backupBusy.value)
            assertNull(vm.importPrompt.value)
            assertEquals(" padded ", repository.getAllPasswords().first().single().password)
        }

    @Test
    fun `legacy JSON under a CSV name imports without a password prompt`() = runTest {
        val uri = Uri.parse("content://test/google.csv")
        val input =
            TrackingInput("\uFEFF[{\"siteName\":\"Legacy\",\"password\":\" \\t \"}]".toByteArray())
        shadowOf(resolver).registerInputStream(uri, input)
        val vm = viewModel()

        vm.importPasswords(uri)
        advanceUntilIdle()

        assertEquals(VaultEvent.ImportSucceeded(1, 0), vm.events.first())
        assertTrue(input.closed)
        assertNull(vm.importPrompt.value)
        assertEquals(" \t ", repository.getAllPasswords().first().single().password)
    }

    @Test
    fun `malformed CSV after a valid row and malformed UTF8 insert nothing and close streams`() =
        runTest {
            val vm = viewModel()
            val malformed = registerInput(CSV + "\nBad,https://example.org,u,\"unterminated,")
            vm.importPasswords(INPUT)
            advanceUntilIdle()
            assertEquals(VaultEvent.ImportFailed, vm.events.first())
            assertTrue(malformed.closed)
            assertTrue(dao.stored.isEmpty())

            val invalidUtf8 = TrackingInput(CSV.toByteArray() + byteArrayOf(0xc3.toByte(), 0x28))
            shadowOf(resolver).registerInputStream(INPUT, invalidUtf8)
            vm.importPasswords(INPUT)
            advanceUntilIdle()
            assertEquals(VaultEvent.ImportFailed, vm.events.first())
            assertTrue(invalidUtf8.closed)
            assertTrue(dao.stored.isEmpty())
            assertFalse(vm.backupBusy.value)
        }

    @Test
    fun `row limit includes invalid CSV records and refuses the entire import`() = runTest {
        val input =
            registerInput(
                "name,url,username,password\n" + "Missing,,u,\n".repeat(BackupCodec.MAX_ENTRIES + 1)
            )
        val vm = viewModel()
        vm.importPasswords(INPUT)
        advanceUntilIdle()

        assertEquals(VaultEvent.ImportTooLarge, vm.events.first())
        assertTrue(input.closed)
        assertTrue(dao.stored.isEmpty())
        assertFalse(vm.backupBusy.value)
    }

    @Test
    fun `picker cancellation and rejected password submissions clear their arrays`() {
        val vm = viewModel(authentication = authorizedExport())
        val first = PASSWORD.toCharArray()
        val second = PASSWORD.toCharArray()
        vm.prepareExport(first)
        assertFalse(first.all { it == '\u0000' })
        vm.prepareExport(second)
        assertTrue(first.all { it == '\u0000' })
        assertFalse(second.all { it == '\u0000' })
        vm.cancelExport()
        assertTrue(second.all { it == '\u0000' })
        val unused = PASSWORD.toCharArray()
        vm.submitImportPassword(unused)
        assertTrue(unused.all { it == '\u0000' })
    }

    @Test
    fun `read failures close streams clear busy and allow retry`() = runTest {
        val failing =
            object : TrackingInput(byteArrayOf()) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    throw IOException("Read failed")
            }
        shadowOf(resolver).registerInputStream(INPUT, failing)
        val vm = viewModel()
        vm.importPasswords(INPUT)
        advanceUntilIdle()
        assertEquals(VaultEvent.ImportFailed, vm.events.first())
        assertTrue(failing.closed)
        assertFalse(vm.backupBusy.value)

        registerInput(CSV)
        vm.importPasswords(INPUT)
        advanceUntilIdle()
        assertEquals(VaultEvent.ImportSucceeded(1, 0), vm.events.first())
    }

    @Test
    fun `encrypted content prompts retries wrong password and ignores duplicate submits`() =
        runTest {
            val backup = BackupCodec.encode(listOf(entry()), PASSWORD.toCharArray(), ITERATIONS)
            val input = registerInput(backup)
            val vm = viewModel()
            vm.importPasswords(INPUT)
            advanceUntilIdle()
            assertTrue(input.closed)
            assertEquals(backup, vm.importPrompt.value?.content)
            assertFalse(vm.backupBusy.value)

            val wrong = "wrong password".toCharArray()
            vm.submitImportPassword(wrong)
            advanceUntilIdle()
            assertTrue(wrong.all { it == '\u0000' })
            assertEquals(true, vm.importPrompt.value?.wrongPassword)
            assertTrue(dao.stored.isEmpty())

            val correct = PASSWORD.toCharArray()
            val duplicate = PASSWORD.toCharArray()
            vm.submitImportPassword(correct)
            vm.submitImportPassword(duplicate)
            assertTrue(duplicate.all { it == '\u0000' })
            advanceUntilIdle()
            assertEquals(VaultEvent.ImportSucceeded(1, 0), vm.events.first())
            assertTrue(correct.all { it == '\u0000' })
            assertNull(vm.importPrompt.value)
            assertEquals(1, dao.stored.size)
        }

    @Test
    fun `cancelled password submission clears secrets before any writes and permits another import`() =
        runTest {
            registerInput(BackupCodec.encode(listOf(entry()), PASSWORD.toCharArray(), ITERATIONS))
            val vm = viewModel()
            vm.importPasswords(INPUT)
            advanceUntilIdle()
            val password = PASSWORD.toCharArray()
            vm.submitImportPassword(password)
            vm.cancelImport()
            advanceUntilIdle()

            assertTrue(password.all { it == '\u0000' })
            assertTrue(dao.stored.isEmpty())
            assertNull(vm.importPrompt.value)
            assertFalse(vm.backupBusy.value)

            registerInput(CSV)
            vm.importPasswords(INPUT)
            advanceUntilIdle()
            assertEquals(VaultEvent.ImportSucceeded(1, 0), vm.events.first())
        }

    @Test
    fun `clearing the ViewModel cancels pending work and wipes export passwords`() = runTest {
        val vm = viewModel(authentication = authorizedExport())
        val password = PASSWORD.toCharArray()
        vm.prepareExport(password)
        val store = ViewModelStore()
        store.put("backup", vm)
        vm.importPasswords(INPUT) // Pending export owns the operation; import is ignored.
        store.clear()
        advanceUntilIdle()

        assertTrue(password.all { it == '\u0000' })
        assertFalse(vm.backupBusy.value)
        assertNull(vm.importPrompt.value)
    }

    @Test
    fun `export is one use encrypted v2 at production KDF cost and closes output`() = runTest {
        repository.insertPassword(entry())
        val output = TrackingOutput()
        shadowOf(resolver).registerOutputStream(OUTPUT, output)
        val auth = authorizedExport()
        val vm = viewModel(authentication = auth)
        val password = PASSWORD.toCharArray()
        vm.prepareExport(password)
        assertTrue(auth.completeExportPicker(true))
        vm.exportPasswords(OUTPUT)
        assertTrue(vm.backupBusy.value)
        vm.exportPasswords(OUTPUT)
        advanceUntilIdle()

        assertEquals(VaultEvent.ExportSucceeded, vm.events.first())
        assertTrue(output.closed)
        assertTrue(password.all { it == '\u0000' })
        assertFalse(vm.backupBusy.value)
        val content = output.toString(Charsets.UTF_8.name())
        assertTrue(content.contains("\"iterations\":600000"))
        assertFalse(content.contains("recovery"))
        assertEquals(listOf(entry()), BackupCodec.decode(content, PASSWORD.toCharArray()))
    }

    @Test
    fun `failed export clears password closes output and does not reuse consent on retry`() =
        runTest {
            repository.insertPassword(entry())
            val output =
                object : TrackingOutput() {
                    override fun write(buffer: ByteArray, offset: Int, length: Int) =
                        throw IOException("Write failed")
                }
            shadowOf(resolver).registerOutputStream(OUTPUT, output)
            val auth = authorizedExport()
            val vm = viewModel(authentication = auth)
            val password = PASSWORD.toCharArray()
            vm.prepareExport(password)
            assertTrue(auth.completeExportPicker(true))
            vm.exportPasswords(OUTPUT)
            advanceUntilIdle()

            assertEquals(VaultEvent.ExportFailed, vm.events.first())
            assertTrue(output.closed)
            assertTrue(password.all { it == '\u0000' })
            assertFalse(vm.backupBusy.value)
            val unused = TrackingOutput()
            shadowOf(resolver).registerOutputStream(OUTPUT, unused)
            vm.exportPasswords(OUTPUT)
            advanceUntilIdle()
            assertFalse(unused.closed)
            assertEquals(0, unused.size())
        }

    @Test
    fun `encryption failure imports no rows and database failure is reported`() = runTest {
        registerInput(CSV + "\nSecond,https://example.org,u,p,n")
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        var lookups = 0
        CryptoManager.setKeyProviderForTesting {
            if (++lookups > 5) throw CryptoException("Vault key unavailable") else key
        }
        val vm = viewModel()
        vm.importPasswords(INPUT)
        advanceUntilIdle()
        assertEquals(VaultEvent.ImportFailed, vm.events.first())
        assertTrue(dao.stored.isEmpty())

        CryptoManager.setKeyProviderForTesting { key }
        val failingDao =
            object : PasswordDao by dao {
                override suspend fun insertAll(passwords: List<PasswordEntity>): List<Long> =
                    throw SQLException("Insert failed")
            }
        val failingVm = viewModel(PasswordRepository(failingDao, mainDispatcher.dispatcher))
        registerInput(CSV)
        failingVm.importPasswords(INPUT)
        advanceUntilIdle()
        assertEquals(VaultEvent.ImportFailed, failingVm.events.first())
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun `foreign legacy backup is rejected without inserting ciphertext`() = runTest {
        val foreign = CryptoManager.encrypt("secret")
        CryptoManager.setKeyProviderForTesting {
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        }
        registerInput("[{\"siteName\":\"Foreign\",\"password\":\"$foreign\"}]")
        val vm = viewModel()
        vm.importPasswords(INPUT)
        advanceUntilIdle()

        assertEquals(VaultEvent.BackupFromOtherDevice, vm.events.first())
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun `imports wait through export authentication consent and picker then resume after cancellation`() =
        runTest {
            val input = registerInput(CSV)
            val auth = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })
            auth.setDestination("home", null)
            auth.onForeground(true)
            val vm = viewModel(authentication = auth)
            auth.request(Action.Export, "home")
            vm.importPasswords(INPUT)
            val request = requireNotNull(auth.beginNativePrompt())
            vm.importPasswords(INPUT)
            auth.nativeResult(request.token, true)
            vm.importPasswords(INPUT)
            assertTrue(auth.startExportPicker(request.token))
            val password = PASSWORD.toCharArray()
            vm.prepareExport(password)
            vm.importPasswords(INPUT)
            assertTrue(auth.completeExportPicker(true))
            vm.importPasswords(INPUT)
            advanceUntilIdle()

            assertFalse(input.closed)
            assertFalse(vm.backupBusy.value)
            assertTrue(dao.stored.isEmpty())
            assertEquals(0, dao.listReads)
            assertFalse(password.all { it == '\u0000' })

            auth.cancelPending()
            vm.importPasswords(INPUT)
            assertTrue(password.all { it == '\u0000' })
            advanceUntilIdle()
            assertEquals(VaultEvent.ImportSucceeded(1, 0), vm.events.first())
            assertTrue(input.closed)
        }

    @Test
    fun `expired picker completion clears password without vault or provider access`() = runTest {
        var now = 1_000L
        val auth = authorizedExport { now }
        val vm = viewModel(authentication = auth)
        val password = PASSWORD.toCharArray()
        val output = TrackingOutput()
        shadowOf(resolver).registerOutputStream(OUTPUT, output)
        vm.prepareExport(password)
        now += 300_000L
        assertFalse(auth.completeExportPicker(true))
        vm.exportPasswords(OUTPUT)
        advanceUntilIdle()

        assertTrue(password.all { it == '\u0000' })
        assertEquals(0, dao.listReads)
        assertFalse(output.closed)
        assertEquals(0, output.size())
        assertFalse(vm.backupBusy.value)
    }

    @Test
    fun `revocation after picker completion denies export before queued collector runs`() =
        runTest {
            val auth = authorizedExport()
            val vm = viewModel(authentication = auth)
            val password = PASSWORD.toCharArray()
            val output = TrackingOutput()
            shadowOf(resolver).registerOutputStream(OUTPUT, output)
            vm.prepareExport(password)
            assertTrue(auth.completeExportPicker(true))
            auth.revoke()
            vm.exportPasswords(OUTPUT)

            assertTrue(password.all { it == '\u0000' })
            assertEquals(0, dao.listReads)
            assertFalse(output.closed)
            assertEquals(0, output.size())
            assertFalse(vm.backupBusy.value)
            advanceUntilIdle()
        }

    @Test
    fun `cancelled native export blocks import until its terminal callback drains`() = runTest {
        var providerOpens = 0
        val input = TrackingInput(CSV.toByteArray())
        shadowOf(resolver).registerInputStreamSupplier(INPUT) {
            providerOpens++
            input
        }
        val auth = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })
        auth.setDestination("home", null)
        auth.onForeground(true)
        val vm = viewModel(authentication = auth)
        auth.request(Action.Export, "home")
        val request = requireNotNull(auth.beginNativePrompt())
        auth.cancelPending()
        assertNull(auth.state.value.pending)
        assertNull(auth.state.value.export)
        assertEquals(request, auth.state.value.nativeRequest)

        vm.importPasswords(INPUT)
        advanceUntilIdle()
        assertEquals(0, providerOpens)
        assertEquals(0, dao.listReads)
        assertTrue(dao.stored.isEmpty())
        assertFalse(vm.backupBusy.value)
        assertFalse(input.closed)

        auth.nativeResult(request.token, false)
        assertNull(auth.state.value.nativeRequest)
        vm.importPasswords(INPUT)
        advanceUntilIdle()
        assertEquals(VaultEvent.ImportSucceeded(1, 0), vm.events.first())
        assertEquals(1, providerOpens)
        assertEquals(1, dao.listReads)
        assertTrue(input.closed)
    }

    private fun authorizedExport(clock: () -> Long = { 1_000L }): EntryAuthenticationViewModel {
        val auth = EntryAuthenticationViewModel({ 30_000L }, clock)
        auth.setDestination("home", null)
        auth.onForeground(true)
        auth.request(Action.Export, "home")
        val request = requireNotNull(auth.beginNativePrompt())
        auth.nativeResult(request.token, true)
        assertTrue(auth.startExportPicker(request.token))
        return auth
    }

    private fun viewModel(
        repo: PasswordRepository = repository,
        authentication: EntryAuthenticationViewModel? = null,
    ) =
        BackupViewModel(
            repo,
            resolver,
            ioDispatcher = StandardTestDispatcher(mainDispatcher.dispatcher.scheduler, name = "io"),
            cpuDispatcher =
                StandardTestDispatcher(mainDispatcher.dispatcher.scheduler, name = "cpu"),
            authentication = authentication,
        )

    private fun registerInput(content: String): TrackingInput =
        TrackingInput(content.toByteArray()).also {
            shadowOf(resolver).registerInputStream(INPUT, it)
        }

    private fun entry() =
        PasswordEntity(
            siteName = "Example",
            username = "alice",
            password = " \t ",
            url = "android://hash@com.example.app/",
            notes = "recovery",
            createdAt = 1,
            updatedAt = 2,
        )

    private open class TrackingInput(content: ByteArray) : ByteArrayInputStream(content) {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    private open class TrackingOutput : ByteArrayOutputStream() {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    private companion object {
        val INPUT: Uri = Uri.parse("content://test/mislabeled.json")
        val OUTPUT: Uri = Uri.parse("content://test/kupass-backup.kupass")
        const val PASSWORD = "correct horse battery"
        const val ITERATIONS = 10_000
        const val CSV = "name,url,username,password,note\nSite,https://example.com,u, padded ,note"
    }
}
