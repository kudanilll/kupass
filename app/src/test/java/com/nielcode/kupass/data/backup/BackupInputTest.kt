package com.nielcode.kupass.data.backup

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupInputTest {
    @Test
    fun `UTF8 BOM and non ASCII values are decoded without changing credential whitespace`() {
        val content = "name,url,username,password\nCafé,https://example.com,用户, \t "
        assertEquals(
            content,
            BackupInput.read(ByteArrayInputStream(("\uFEFF" + content).toByteArray())),
        )
    }

    @Test
    fun `invalid UTF8 is rejected rather than replacing password bytes`() {
        listOf(
                byteArrayOf(0xc3.toByte(), 0x28),
                byteArrayOf(0xe2.toByte(), 0x82.toByte()),
                byteArrayOf(0xc0.toByte(), 0xaf.toByte()),
                byteArrayOf(0xed.toByte(), 0xa0.toByte(), 0x80.toByte()),
            )
            .forEach { bytes ->
                assertThrows(BackupException.Malformed::class.java) {
                    BackupInput.read(ByteArrayInputStream(bytes))
                }
            }
    }

    @Test
    fun `exact byte cap works and one extra byte is refused`() {
        assertEquals(
            BackupInput.MAX_BYTES,
            BackupInput.read(RepeatedInput(BackupInput.MAX_BYTES)).length,
        )
        assertThrows(BackupException.TooLarge::class.java) {
            BackupInput.read(RepeatedInput(BackupInput.MAX_BYTES + 1))
        }
    }

    private class RepeatedInput(private var remaining: Int) : InputStream() {
        override fun read(): Int = if (remaining-- > 0) 'a'.code else -1

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining == 0) return -1
            val count = minOf(remaining, length)
            buffer.fill('a'.code.toByte(), offset, offset + count)
            remaining -= count
            return count
        }
    }
}
