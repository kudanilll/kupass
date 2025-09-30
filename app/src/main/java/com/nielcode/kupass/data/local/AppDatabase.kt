package com.nielcode.kupass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nielcode.kupass.data.local.dao.SiteDao
import com.nielcode.kupass.data.local.entity.CredentialEntity
import com.nielcode.kupass.data.local.entity.SiteEntity
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [SiteEntity::class, CredentialEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                System.loadLibrary("sqlcipher")

                // Hook for license/PRAGMA
                val hook = object : SQLiteDatabaseHook {
                    override fun preKey(connection: SQLiteConnection) {
                    }

                    override fun postKey(connection: SQLiteConnection) {
                    }
                }

                val factory = SupportOpenHelperFactory( // Zetetic factory for Room
                    passphrase,
                    hook,      // CursorFactory
                    false
                )

                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kupass.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
