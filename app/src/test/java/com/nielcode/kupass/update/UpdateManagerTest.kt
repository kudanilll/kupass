package com.nielcode.kupass.update

import com.nielcode.kupass.update.UpdateState.Available
import com.nielcode.kupass.update.UpdateState.Downloading
import com.nielcode.kupass.update.UpdateState.Failed
import com.nielcode.kupass.update.UpdateState.Idle
import com.nielcode.kupass.update.UpdateState.Installing
import com.nielcode.kupass.update.UpdateState.NeedsPermission
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateManagerTest {

    private val update =
        AvailableUpdate(
            version = AppVersion(3, 1, 0, beta = 2),
            tag = "v3.1.0-beta.2",
            apkUrl = "https://example.com/kupass.apk",
            apkSize = 10,
            checksumUrl = "https://example.com/kupass.apk.sha256",
            pageUrl = "https://example.com/release",
        )
    private val apk = File("kupass-update.apk")

    private var offered: AvailableUpdate? = update
    private var offers = 0
    private var downloadGate = CompletableDeferred<File?>()
    private var reportProgress: ((Int) -> Unit)? = null
    private val installer =
        object : PackageUpdater {
            var allowed = true
            var accepts = true
            val installed = mutableListOf<File>()

            override fun canInstall() = allowed

            override fun install(apk: File): Boolean {
                installed += apk
                return accepts
            }
        }

    private fun TestScope.manager(): UpdateManager {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return UpdateManager(
            offer = {
                offers++
                offered
            },
            download = { _, progress ->
                reportProgress = progress
                progress(40)
                downloadGate.await()
            },
            installer = installer,
            // Not backgroundScope: advanceUntilIdle() leaves background work unrun.
            scope = CoroutineScope(dispatcher),
            io = dispatcher,
        )
    }

    private fun TestScope.offeredManager(): UpdateManager =
        manager().also {
            it.onAppOpen()
            advanceUntilIdle()
        }

    @Test
    fun `opening the app offers the update the checker found`() = runTest {
        val manager = offeredManager()

        assertEquals(Available(update), manager.state.value)
    }

    @Test
    fun `nothing found leaves the dialog hidden`() = runTest {
        offered = null

        assertEquals(Idle, offeredManager().state.value)
    }

    @Test
    fun `Later or back hides it until the app opens again`() = runTest {
        val manager = offeredManager()

        manager.close()
        assertEquals(Idle, manager.state.value)

        manager.onAppOpen()
        advanceUntilIdle()
        assertEquals(Available(update), manager.state.value)
        assertEquals(2, offers)
    }

    @Test
    fun `a restored screen offers it only if this process has not checked yet`() = runTest {
        // Android may restart the app, e.g. after "Install unknown apps" changes.
        val manager = manager()
        manager.onAppOpen(restored = true)
        advanceUntilIdle()
        assertEquals(Available(update), manager.state.value)

        // A rotation after closing it doesn't bring it back.
        manager.close()
        manager.onAppOpen(restored = true)
        advanceUntilIdle()
        assertEquals(Idle, manager.state.value)
        assertEquals(1, offers)
    }

    @Test
    fun `opening again while it is shown does not check again`() = runTest {
        val manager = offeredManager()

        manager.onAppOpen()
        advanceUntilIdle()

        assertEquals(1, offers)
    }

    @Test
    fun `a verified download is installed with progress shown on the way`() = runTest {
        val manager = offeredManager()

        manager.download()
        runCurrent()
        assertEquals(Downloading(update, 40), manager.state.value)

        downloadGate.complete(apk)
        advanceUntilIdle()
        assertEquals(Installing(update), manager.state.value)
        assertEquals(listOf(apk), installer.installed)
    }

    @Test
    fun `without the install permission it waits until the user grants it`() = runTest {
        installer.allowed = false
        val manager = offeredManager()
        manager.download()
        downloadGate.complete(apk)
        advanceUntilIdle()
        assertEquals(NeedsPermission(update, apk), manager.state.value)

        manager.onResume()
        advanceUntilIdle()
        assertEquals(NeedsPermission(update, apk), manager.state.value)

        installer.allowed = true
        manager.onResume()
        advanceUntilIdle()
        assertEquals(Installing(update), manager.state.value)
        assertEquals(listOf(apk), installer.installed)
    }

    @Test
    fun `a failed download fails, and Try again downloads again`() = runTest {
        val manager = offeredManager()
        manager.download()
        downloadGate.complete(null)
        advanceUntilIdle()
        assertEquals(Failed(update), manager.state.value)

        downloadGate = CompletableDeferred(apk)
        manager.download()
        advanceUntilIdle()
        assertEquals(Installing(update), manager.state.value)
    }

    @Test
    fun `an install the system refuses fails`() = runTest {
        installer.accepts = false
        val manager = offeredManager()

        manager.download()
        downloadGate.complete(apk)
        advanceUntilIdle()

        assertEquals(Failed(update), manager.state.value)
    }

    @Test
    fun `a failure reported by the installer fails, but only while installing`() = runTest {
        val manager = offeredManager()
        manager.onInstallFailed()
        assertEquals(Available(update), manager.state.value)

        manager.download()
        downloadGate.complete(apk)
        advanceUntilIdle()
        manager.onInstallFailed()
        assertEquals(Failed(update), manager.state.value)
    }

    @Test
    fun `cancelling a download offers the update again, and late progress is ignored`() = runTest {
        val manager = offeredManager()
        manager.download()
        runCurrent()

        manager.cancelDownload()
        reportProgress?.invoke(80)
        downloadGate.complete(apk)
        advanceUntilIdle()

        assertEquals(Available(update), manager.state.value)
        assertEquals(emptyList<File>(), installer.installed)
    }
}
