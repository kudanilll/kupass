package com.nielcode.kupass.data.repository

import com.nielcode.kupass.model.SiteAccount
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PasswordRepository {
    fun observeAll(): Flow<List<SiteAccount>>
    suspend fun addOrUpdateSite(
        site: String,
        note: String?
    ): Long

    suspend fun addCredential(
        siteId: Long,
        username: String,
        plainPassword: String,
        lastUpdated: Date = Date()
    )

    suspend fun deleteSite(siteId: Long)
}
