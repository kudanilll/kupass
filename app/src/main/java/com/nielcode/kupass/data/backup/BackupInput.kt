package com.nielcode.kupass.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Bounded Unicode input: UTF-8 or BOM-marked UTF-16; malformed credentials are never replaced. */
internal object BackupInput {
    const val MAX_BYTES = 32 * 1024 * 1024

    fun read(input: InputStream): String {
        val out = WipeableBuffer()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                requireSize(out.size() + read)
                out.write(buffer, 0, read)
            }
            return out.decode()
        } finally {
            buffer.fill(0)
            out.clear()
        }
    }

    fun requireSize(bytes: Int) {
        if (bytes > MAX_BYTES) throw BackupException.TooLarge()
    }

    fun utf8(bytes: ByteArray, size: Int = bytes.size): String = decode(bytes, size, Charsets.UTF_8)

    private fun decode(bytes: ByteArray, size: Int, charset: Charset): String =
        try {
            charset
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, 0, size))
                .toString()
        } catch (_: CharacterCodingException) {
            throw BackupException.Malformed()
        }

    private class WipeableBuffer : ByteArrayOutputStream() {
        fun decode(): String {
            val charset =
                when {
                    count >= 2 && buf[0] == 0xFF.toByte() && buf[1] == 0xFE.toByte() ->
                        Charsets.UTF_16LE
                    count >= 2 && buf[0] == 0xFE.toByte() && buf[1] == 0xFF.toByte() ->
                        Charsets.UTF_16BE
                    else -> Charsets.UTF_8
                }
            return decode(buf, count, charset).removePrefix("\uFEFF")
        }

        fun clear() {
            buf.fill(0)
            reset()
        }
    }
}
