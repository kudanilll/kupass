package com.nielcode.kupass.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** Bounded, strict UTF-8 reads shared by backup and CSV input. The caller owns the stream. */
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

    fun utf8(bytes: ByteArray, size: Int = bytes.size): String =
        try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, 0, size))
                .toString()
        } catch (_: CharacterCodingException) {
            throw BackupException.Malformed()
        }

    private class WipeableBuffer : ByteArrayOutputStream() {
        fun decode(): String = utf8(buf, count).removePrefix("\uFEFF")

        fun clear() {
            buf.fill(0)
            reset()
        }
    }
}
