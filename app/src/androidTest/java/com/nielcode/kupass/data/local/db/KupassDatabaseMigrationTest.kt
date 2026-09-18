package com.nielcode.kupass.data.local.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that a database created at the oldest shipped schema (version 1) still opens with the
 * current schema and every registered migration, without losing rows.
 *
 * When adding a migration: keep [migrateAllFromVersion1], and add a focused test that creates the
 * previous version, inserts representative rows, and calls `helper.runMigrationsAndValidate`.
 */
@RunWith(AndroidJUnit4::class)
class KupassDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, KupassDatabase::class.java)

    @Test
    fun migrateAllFromVersion1() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO passwords (site_name, username, password, url, notes, created_at, updated_at) " +
                    "VALUES ('Example', 'user', 'ciphertext', 'https://example.com', 'note', 1000, 2000)"
            )
            close()
        }

        val db =
            Room.databaseBuilder(instrumentation.targetContext, KupassDatabase::class.java, TEST_DB)
                .addMigrations(*KupassDatabase.MIGRATIONS)
                .build()
        try {
            // Room validates the schema against the current entities when it opens.
            val rows = runBlocking { db.passwordDao().getAll().first() }
            assertEquals(1, rows.size)
            assertEquals("Example", rows.single().siteName)
            assertEquals(1000L, rows.single().createdAt)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
