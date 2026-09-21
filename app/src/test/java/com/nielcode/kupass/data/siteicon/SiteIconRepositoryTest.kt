package com.nielcode.kupass.data.siteicon

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
