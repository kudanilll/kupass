package com.nielcode.kupass.utils

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * A utility object for handling file I/O operations using ContentResolver and Uri.
 * This is the modern and recommended way to interact with files provided by the
 * Storage Access Framework.
 */
object FileHandler {

    /**
     * Writes the given content to a file specified by the Uri.
     *
     * @param context The application context.
     * @param uri The Uri of the file to write to, typically obtained from ACTION_CREATE_DOCUMENT.
     * @param content The String content to write to the file.
     * @return `true` if the write operation was successful, `false` otherwise.
     */
    fun writeFileContent(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads the content from a file specified by the Uri.
     *
     * @param context The application context.
     * @param uri The Uri of the file to read from, typically obtained from ACTION_GET_CONTENT.
     * @return The content of the file as a String, or `null` if the read operation fails.
     */
    fun readFileContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                reader?.readText()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}