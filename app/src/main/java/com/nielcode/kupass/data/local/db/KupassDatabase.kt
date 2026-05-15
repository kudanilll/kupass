package com.nielcode.kupass.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the Kupass app.
 * Uses a singleton pattern to prevent multiple database instances.
 */
@Database(entities = [PasswordEntity::class], version = 1, exportSchema = false)
abstract class KupassDatabase : RoomDatabase() {

    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: KupassDatabase? = null

        fun getInstance(context: Context): KupassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KupassDatabase::class.java,
                    "kupass_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
