package com.nielcode.kupass.data.siteicon

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.nielcode.kupass.data.local.cache.SiteIconStore
import com.nielcode.kupass.security.CryptoManager
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.KeyGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SiteIconRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val fetches = AtomicInteger()
    private var result: IconFetchResult = IconFetchResult.Found(byteArrayOf(1))
    private var now = 0L
    /** Real threads for the test where a fetch blocks until every caller waits on it. */
    private val threads = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    @After
    fun closeThreads() {
        threads.close()
    }

    private fun repository(
        source: IconSource? = IconSource {
            fetches.incrementAndGet()
            result
        },
        ioDispatcher: CoroutineDispatcher = dispatcher,
    ): SiteIconRepository {
        return SiteIconRepository(
            source = source,
            scope = scope,
            decode = { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap() },
            ioDispatcher = ioDispatcher,
            clock = { now },
        )
    }

    @get:Rule val folder = TemporaryFolder()

    /** A repository backed by the same on-disk store, as after an app restart. */
    private fun persistent(): SiteIconRepository {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val file = File(folder.root, "site-icons")
        return SiteIconRepository(
            source = {
                fetches.incrementAndGet()
                result
            },
            scope = scope,
            store = SiteIconStore(file, clock = { now }),
            decode = { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap() },
            ioDispatcher = dispatcher,
            clock = { now },
        )
    }

    @Test
    fun `an icon fetched once is served from the store after a restart`() = scope.runTest {
        assertNotNull(persistent().load("github.com"))
        advanceUntilIdle() // lets the batched save run

        val afterRestart =
            SiteIconRepository(
                source = {
                    fetches.incrementAndGet()
                    result
                },
                scope = scope,
                store = SiteIconStore(File(folder.root, "site-icons"), clock = { now }),
                decode = { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap() },
                ioDispatcher = dispatcher,
                clock = { now },
            )

        assertNotNull(afterRestart.load("github.com"))
        assertEquals(1, fetches.get())
    }

    @Test
    fun `misses and failures are not stored`() = scope.runTest {
        result = IconFetchResult.NotFound
        assertNull(persistent().load("nothing.example"))
        result = IconFetchResult.Failed
        assertNull(persistent().load("down.example"))
        advanceUntilIdle()

        assertTrue(SiteIconStore(File(folder.root, "site-icons")).load().isEmpty())
    }

    @Test
    fun `does nothing without an api key`() = scope.runTest {
        val icons = repository(source = null)

        assertNull(icons.load("github.com"))
        assertEquals(0, fetches.get())
    }

    @Test
    fun `loads once, then serves from memory`() = scope.runTest {
        val icons = repository()

        val first = icons.load("github.com")
        val second = icons.load("github.com")

        assertNotNull(first)
        assertSame(first, second)
        assertSame(first, icons.peek("github.com"))
        assertEquals(1, fetches.get())
    }

    @Test
    fun `concurrent loads of one domain share a fetch`() = scope.runTest {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val icons =
            repository(
                ioDispatcher = threads,
                source = {
                    fetches.incrementAndGet()
                    started.countDown()
                    release.await()
                    result
                },
            )

        val loads = List(3) { async(threads) { icons.load("github.com") } }
        withContext(threads) { started.await() }
        release.countDown()
        val results = loads.map { it.await() }

        assertEquals(1, fetches.get())
        assertTrue(results.all { it === results.first() && it != null })
    }

    @Test
    fun `remembers misses and retries failures sooner`() = scope.runTest {
        val icons = repository()

        result = IconFetchResult.NotFound
        assertNull(icons.load("none.example"))
        now += 60 * 60 * 1000L
        assertNull(icons.load("none.example"))
        assertEquals(1, fetches.get())

        result = IconFetchResult.Failed
        assertNull(icons.load("down.example"))
        now += 5 * 60 * 1000L + 1
        result = IconFetchResult.Found(byteArrayOf(1))
        assertNotNull(icons.load("down.example"))
        assertEquals(3, fetches.get())
    }
}
