package com.nielcode.kupass.data.backup

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nielcode.kupass.data.local.db.PasswordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the production KDF cost on a real device: correctness plus a rough time budget. */
@RunWith(AndroidJUnit4::class)
class BackupCodecDeviceTest {

    @Test
    fun productionIterationsRoundTripWithinBudget() {
        val entries = (1..200).map { PasswordEntity(siteName = "site$it", username = "user$it", password = "pw-$it", notes = "n$it") }
        val password = "correct horse battery"

        val start = SystemClock.elapsedRealtime()
        val backup = BackupCodec.encode(entries, password.toCharArray())
        val encodeMs = SystemClock.elapsedRealtime() - start
        val restored = BackupCodec.decode(backup, password.toCharArray())
        val totalMs = SystemClock.elapsedRealtime() - start
        Log.i("BackupCodecDeviceTest", "PBKDF2 ${BackupCodec.DEFAULT_ITERATIONS} iterations: encode=${encodeMs}ms, encode+decode=${totalMs}ms")

        assertEquals(entries.map { it.copy(id = 0) }, restored)
        // Generous budget for slow emulators; a real regression (e.g. 10x iterations) would blow it.
        assertTrue("backup round trip took ${totalMs}ms", totalMs < 30_000)
    }
}
