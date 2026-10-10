package com.nielcode.kupass.ui.screens

import android.net.Uri
import com.nielcode.kupass.data.backup.GooglePasswordCsv
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.CryptoException
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.testing.FakePasswordDao
import com.nielcode.kupass.testing.MainDispatcherRule
import com.nielcode.kupass.ui.screens.data.BackupViewModel
import com.nielcode.kupass.ui.screens.detail.DeleteState
import com.nielcode.kupass.ui.screens.detail.PasswordDetailViewModel
import com.nielcode.kupass.ui.screens.editor.PasswordEditorViewModel
import com.nielcode.kupass.ui.screens.editor.SaveState
import com.nielcode.kupass.ui.screens.home.HomeViewModel
import javax.crypto.KeyGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

@RunWith(RobolectricTestRunner::class)
class ViewModelsTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private lateinit var dao: FakePasswordDao
    private lateinit var repository: PasswordRepository

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        dao = FakePasswordDao()
        repository = PasswordRepository(dao, cryptoDispatcher = mainDispatcher.dispatcher)
    }

    @Test
    fun `editor saves a new entry trimmed and encrypted at rest`() = runTest {
        val vm = PasswordEditorViewModel(repository)

        vm.savePassword(
            siteName = "  GitHub ",
            username = " me ",
            password = "s3cret",
            url = "",
            notes = "",
        )

        assertEquals(SaveState.Success, vm.saveState.value)
        val stored = dao.stored.single()
        assertEquals("GitHub", CryptoManager.decrypt(stored.siteName))
        assertEquals("me", CryptoManager.decrypt(stored.username))
        listOf(stored.siteName, stored.username, stored.password).forEach {
            assertTrue(it.startsWith(CryptoManager.PREFIX_V2))
        }
    }

    @Test
    fun `editor updates an existing entry`() = runTest {
        val id = repository.insertPassword(PasswordEntity(siteName = "Old", password = "a"))
        val vm = PasswordEditorViewModel(repository)
        vm.loadPassword(id)

        vm.savePassword(siteName = "New", username = "", password = "b", url = "", notes = "")

        assertEquals(SaveState.Success, vm.saveState.value)
        assertEquals(1, dao.stored.size)
        assertEquals("New", CryptoManager.decrypt(dao.stored.single().siteName))
        assertEquals("b", CryptoManager.decrypt(dao.stored.single().password))
    }

    @Test
    fun `editing another field preserves imported username password notes ID and creation time`() =
        runTest {
            val notes = " \nformatted note\n\t "
            val csv =
                "name,url,username,password,note\n" +
                    "Imported,https://example.com, me , \t ,\"$notes\""
            assertEquals(
                1,
                repository.importPasswords(GooglePasswordCsv.decode(csv).entries).imported,
            )
            val original = repository.getAllPasswords().first().single()
            val vm = PasswordEditorViewModel(repository, requestedId = original.id)
            vm.loadPassword(original.id)

            vm.savePassword(
                "Renamed",
                original.username,
                original.password,
                original.url,
                original.notes,
            )

            assertEquals(SaveState.Success, vm.saveState.value)
            val saved = repository.getPasswordById(original.id).first()
            assertEquals(
                original.copy(siteName = "Renamed", updatedAt = saved?.updatedAt ?: 0),
                saved,
            )
            assertEquals(" me ", saved?.username)
            assertEquals(" \t ", saved?.password)
            assertEquals(notes, saved?.notes)
            assertEquals(1, dao.stored.size)
        }

    @Test
    fun `home lists decrypted entries and filters by search`() = runTest {
        repository.insertPassword(
            PasswordEntity(siteName = "Google", username = "alice", password = "p1")
        )
        repository.insertPassword(
            PasswordEntity(siteName = "GitHub", username = "bob", password = "p2")
        )
        val vm = HomeViewModel(repository, filterDispatcher = mainDispatcher.dispatcher)
        backgroundScope.launchCollect(vm)

        assertEquals(listOf("GitHub", "Google"), vm.passwords.value.map { it.siteName })
        assertEquals(listOf("p2", "p1"), vm.passwords.value.map { it.password })

        vm.onSearchQueryChange("ali")
        assertEquals(listOf("Google"), vm.passwords.value.map { it.siteName })
    }

    @Test
    fun `detail deletes the loaded entry`() = runTest {
        val id = repository.insertPassword(PasswordEntity(siteName = "Temp", password = "x"))
        val vm = PasswordDetailViewModel(repository)
        vm.loadPassword(id)
        assertEquals("Temp", vm.password.value?.siteName)

        vm.deletePassword()

        assertEquals(DeleteState.Success, vm.deleteState.value)
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun `relocked detail drops plaintext and cancels its live repository subscription`() = runTest {
        val id = repository.insertPassword(PasswordEntity(siteName = "Entry", password = "secret"))
        val vm = PasswordDetailViewModel(repository)
        vm.loadPassword(id)
        assertEquals(1, dao.activeDetailReads)
        vm.clearSensitiveState()
        assertEquals(0, dao.activeDetailReads)
        assertNull(vm.password.value)
        repository.updatePassword(PasswordEntity(id = id, siteName = "Changed", password = "new"))
        assertNull(vm.password.value)
    }

    @Test
    fun `export of an empty vault emits NothingToExport`() = runTest {
        val auth = authorizedExport()
        val vm =
            BackupViewModel(
                repository,
                RuntimeEnvironment.getApplication().contentResolver,
                authentication = auth,
            )

        vm.prepareExport("correct horse".toCharArray())
        assertTrue(auth.completeExportPicker(true))
        vm.exportPasswords(Uri.parse("content://test/backup.json"))

        assertEquals(VaultEvent.NothingToExport, vm.events.first())
    }

    @Test
    fun `unauthorized export never snapshots vault or opens a provider and wipes password`() =
        runTest {
            val vm =
                BackupViewModel(repository, RuntimeEnvironment.getApplication().contentResolver)
            val password = "correct horse".toCharArray()
            vm.prepareExport(password)
            vm.exportPasswords(Uri.parse("content://provider-that-must-not-be-called/backup.json"))
            assertEquals(0, dao.listReads)
            assertTrue(password.all { it == '\u0000' })
            assertFalse(vm.backupBusy.value)
        }

    @Test
    fun `revoked export consent clears pending password and no vault snapshot occurs`() = runTest {
        val auth = authorizedExport()
        val vm =
            BackupViewModel(
                repository,
                RuntimeEnvironment.getApplication().contentResolver,
                authentication = auth,
            )
        val password = "correct horse".toCharArray()
        vm.prepareExport(password)
        auth.revoke()
        assertTrue(password.all { it == '\u0000' })
        assertFalse(auth.completeExportPicker(true))
        vm.exportPasswords(Uri.parse("content://provider-that-must-not-be-called/backup.json"))
        assertEquals(0, dao.listReads)
    }

    @Test
    fun `positive editor with missing or unreadable account can never insert`() = runTest {
        val missing = PasswordEditorViewModel(repository, requestedId = 999L)
        missing.savePassword("Missing", "", "secret", "", "")
        assertEquals(SaveState.Error, missing.saveState.value)
        missing.loadPassword(999L)
        missing.savePassword("Missing", "", "secret", "", "")
        assertTrue(missing.loadFailed.value)
        assertTrue(dao.stored.isEmpty())

        val id = repository.insertPassword(PasswordEntity(siteName = "Old", password = "secret"))
        val original = dao.stored.single()
        CryptoManager.setKeyProviderForTesting { throw CryptoException("Unavailable") }
        val unreadable = PasswordEditorViewModel(repository, requestedId = id)
        unreadable.loadPassword(id)
        unreadable.savePassword("Replacement", "", "different", "", "")
        assertTrue(unreadable.loadFailed.value)
        assertEquals(SaveState.Error, unreadable.saveState.value)
        assertEquals(listOf(original), dao.stored)
    }

    private fun authorizedExport(): EntryAuthenticationViewModel {
        val auth = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })
        auth.setDestination("home", null)
        auth.onForeground(true)
        auth.request(Action.Export, "home")
        val request = requireNotNull(auth.beginNativePrompt())
        auth.nativeResult(request.token, true)
        assertTrue(auth.startExportPicker(request.token))
        return auth
    }

    @Test
    fun `editor reports an error instead of crashing when encryption fails`() = runTest {
        CryptoManager.setKeyProviderForTesting { throw CryptoException("Vault key unavailable") }
        val vm = PasswordEditorViewModel(repository)

        vm.savePassword(siteName = "X", username = "", password = "y", url = "", notes = "")

        assertTrue(vm.saveState.value is SaveState.Error)
        assertTrue(dao.stored.isEmpty())
    }

    private fun CoroutineScope.launchCollect(vm: HomeViewModel) {
        launch(mainDispatcher.dispatcher) { vm.passwords.collect {} }
    }
}
