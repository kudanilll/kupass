package com.nielcode.kupass.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.nielcode.kupass.data.local.entity.CredentialEntity
import com.nielcode.kupass.data.local.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

data class SiteWithCreds(
    @Embedded val site: SiteEntity,
    @Relation(parentColumn = "id", entityColumn = "siteId")
    val creds: List<CredentialEntity>
)

@Dao
interface SiteDao {
    @Transaction
    @Query("SELECT * FROM sites ORDER BY site COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SiteWithCreds>>

    @Transaction
    @Query("SELECT * FROM sites ORDER BY site COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<SiteWithCreds>

    @Insert
    suspend fun insertSite(site: SiteEntity): Long

    @Insert
    suspend fun insertCred(cred: CredentialEntity): Long

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("DELETE FROM credentials WHERE id = :credId")
    suspend fun deleteCredential(credId: Long)

    @Query("DELETE FROM credentials WHERE siteId = :siteId AND username = :username")
    suspend fun deleteCredential(siteId: Long, username: String)
}
