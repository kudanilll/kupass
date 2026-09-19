package com.nielcode.kupass.ui.screens

import android.net.Uri
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.testing.FakePasswordDao
import com.nielcode.kupass.testing.MainDispatcherRule
import com.nielcode.kupass.ui.screens.data.BackupViewModel
import com.nielcode.kupass.ui.screens.detail.DeleteState
import com.nielcode.kupass.ui.screens.detail.PasswordDetailViewModel
import com.nielcode.kupass.ui.screens.editor.PasswordEditorViewModel
import com.nielcode.kupass.ui.screens.editor.SaveState
import com.nielcode.kupass.ui.screens.home.HomeViewModel
import com.nielcode.kupass.ui.screens.home.VaultEvent
import com.nielcode.kupass.utils.CryptoException
import com.nielcode.kupass.utils.CryptoManager
import javax.crypto.KeyGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `home lists decrypted entries and filters by search`() = runTest {
        repository.insertPassword(
            PasswordEntity(siteName = "Google", username = "alice", password = "p1")
        )
        repository.insertPassword(
            PasswordEntity(siteName = "GitHub", username = "bob", password = "p2")
        )
        val vm = HomeViewModel(repository)
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
    fun `export of an empty vault emits NothingToExport`() = runTest {
        val vm = BackupViewModel(repository, RuntimeEnvironment.getApplication().contentResolver)

        vm.prepareExport("correct horse".toCharArray())
        vm.exportPasswords(Uri.parse("content://test/backup.json"))

        assertEquals(VaultEvent.NothingToExport, vm.events.first())
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
