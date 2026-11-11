package com.nielcode.kupass.update

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDownloaderTest {

    private val apk = Random(7).nextBytes(300 * 1024)
    private var servedApk = apk
    private var checksum = "${sha256(apk)}  kupass-v3.1.0-beta.2.apk\n"
    private var apkStatus = 200
    private val requested = mutableListOf<String>()

    private val server =
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            createContext("/") { exchange ->
                val path = exchange.requestURI.path
                requested += path
                when (path) {
                    "/download/kupass.apk" -> {
                        // Like GitHub: the asset link redirects to the storage host.
                        exchange.responseHeaders.add("Location", "/storage/kupass.apk")
                        exchange.sendResponseHeaders(302, -1)
                    }
                    "/storage/kupass.apk" -> {
                        exchange.sendResponseHeaders(apkStatus, servedApk.size.toLong())
                        exchange.responseBody.use { it.write(servedApk) }
                    }
                    "/download/kupass.apk.sha256" -> {
                        val bytes = checksum.toByteArray()
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    }
                    else -> exchange.sendResponseHeaders(404, -1)
                }
                exchange.close()
            }
            start()
        }

    private val base = "http://127.0.0.1:${server.address.port}"
    private val dir = Files.createTempDirectory("updates").toFile()
    private val downloader = UpdateDownloader(dir)
    private val update =
        AvailableUpdate(
            version = AppVersion(3, 1, 0, beta = 2),
            tag = "v3.1.0-beta.2",
            apkUrl = "$base/download/kupass.apk",
            apkSize = apk.size.toLong(),
            checksumUrl = "$base/download/kupass.apk.sha256",
            pageUrl = "$base/page",
        )

    @After
    fun stop() {
        server.stop(0)
        dir.deleteRecursively()
    }

    @Test
    fun `a matching APK is downloaded, following the asset redirect, with progress`() {
        val progress = mutableListOf<Int>()

        val file = runBlocking { downloader.download(update) { progress += it } }

        assertArrayEquals(apk, requireNotNull(file).readBytes())
        assertEquals(dir, file.parentFile)
        assertEquals(progress.sorted().distinct(), progress)
        assertEquals(100, progress.last())
    }

    @Test
    fun `a checksum in upper case or without a file name is accepted`() {
        checksum = sha256(apk).uppercase()

        assertTrue(runBlocking { downloader.download(update) } != null)
    }

    @Test
    fun `an APK that does not match the checksum is deleted`() {
        servedApk = apk.copyOf().also { it[1000] = (it[1000] + 1).toByte() }

        assertNull(runBlocking { downloader.download(update) })
        assertEmpty(dir)
    }

    @Test
    fun `an APK of another size than the release lists is refused`() {
        servedApk = apk + byteArrayOf(1)
        assertNull(runBlocking { downloader.download(update) })

        servedApk = apk.copyOf(apk.size - 1)
        assertNull(runBlocking { downloader.download(update) })
        assertEmpty(dir)
    }

    @Test
    fun `a malformed checksum stops before the APK is requested`() {
        checksum = "not a checksum"

        assertNull(runBlocking { downloader.download(update) })
        assertTrue(requested.none { it.endsWith(".apk") })
    }

    @Test
    fun `a server error is a failure`() {
        apkStatus = 500

        assertNull(runBlocking { downloader.download(update) })
        assertEmpty(dir)
    }

    @Test
    fun `links that are not HTTPS, a missing size, or an oversized APK are never requested`() {
        val cases =
            listOf(
                update.copy(apkUrl = "http://example.com/kupass.apk"),
                update.copy(checksumUrl = "http://example.com/kupass.apk.sha256"),
                update.copy(apkSize = 0),
                update.copy(apkSize = 200L * 1024 * 1024),
            )

        cases.forEach { assertNull(it.toString(), runBlocking { downloader.download(it) }) }
        assertTrue(requested.isEmpty())
    }

    @Test
    fun `cancelling stops the download and deletes the partial file`() {
        runBlocking {
            lateinit var job: Job
            job =
                launch(start = CoroutineStart.LAZY) {
                    downloader.download(update) { job.cancel() }
                }
            job.start()
            job.join()
        }

        assertEmpty(dir)
    }

    @Test
    fun `an earlier download left in the folder is removed`() {
        File(dir, "kupass-update.apk").writeBytes(byteArrayOf(1, 2, 3))
        File(dir, "old.apk").writeBytes(byteArrayOf(4))

        val file = runBlocking { downloader.download(update) }

        assertEquals(listOf(requireNotNull(file)), dir.listFiles()?.toList())
    }

    private fun assertEmpty(dir: File) = assertEquals(emptyList<File>(), dir.listFiles()?.toList())

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(Locale.ROOT, it)
        }
}
