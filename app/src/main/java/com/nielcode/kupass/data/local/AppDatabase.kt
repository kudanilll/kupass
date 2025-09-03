package com.nielcode.kupass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nielcode.kupass.data.local.dao.SiteDao
import com.nielcode.kupass.data.local.entity.CredentialEntity
import com.nielcode.kupass.data.local.entity.SiteEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [SiteEntity::class, CredentialEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                SQLiteDatabase.loadLibs(context)
                val factory = SupportFactory(passphrase)

                Room.databaseBuilder(context, AppDatabase::class.java, "kupass.db")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration() // replace with migration once it is stable
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
