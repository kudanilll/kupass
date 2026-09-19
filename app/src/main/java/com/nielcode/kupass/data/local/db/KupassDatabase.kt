package com.nielcode.kupass.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Room database for the Kupass app. Uses a singleton pattern to prevent multiple database
 * instances.
 *
 * Schema changes MUST bump [version], add a [Migration] to [MIGRATIONS], and extend
 * `KupassDatabaseMigrationTest`. Destructive fallback is never allowed: it would wipe the vault.
 */
@Database(entities = [PasswordEntity::class], version = 1, exportSchema = true)
abstract class KupassDatabase : RoomDatabase() {

    abstract fun passwordDao(): PasswordDao

    companion object {
        const val DATABASE_NAME = "kupass_database"

        /** Every migration, in order. Empty while the schema is still at version 1. */
        val MIGRATIONS: Array<Migration> = arrayOf()

        @Volatile private var INSTANCE: KupassDatabase? = null

        // The spread copies MIGRATIONS once per process, when the database is first opened.
        @Suppress("SpreadOperator")
        fun getInstance(context: Context): KupassDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                                context.applicationContext,
                                KupassDatabase::class.java,
                                DATABASE_NAME,
                            )
                            .addMigrations(*MIGRATIONS)
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
