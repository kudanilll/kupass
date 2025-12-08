package com.nielcode.kupass.data.repository

import com.nielcode.kupass.core.crypto.CryptoManager
import com.nielcode.kupass.data.local.dao.SiteDao
import com.nielcode.kupass.data.local.dao.SiteWithCreds
import com.nielcode.kupass.data.local.entity.CredentialEntity
import com.nielcode.kupass.data.local.entity.SiteEntity
import com.nielcode.kupass.model.SiteAccount
import com.nielcode.kupass.model.UserCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class PasswordRepositoryImpl(
    private val dao: SiteDao,
    private val crypto: CryptoManager
) : PasswordRepository {

    override fun observeAll(): Flow<List<SiteAccount>> =
        dao.observeAll().map { list -> list.map { it.toDomain(crypto) } }

    override suspend fun addOrUpdateSite(site: String, note: String?): Long {
        // For a simple example: always insert a new site (can be upsert if necessary)
        return dao.insertSite(SiteEntity(site = site, note = note))
    }

    override suspend fun addCredential(
        siteId: Long,
        username: String,
        plainPassword: String,
        lastUpdated: Date
    ) {
        val aad = "$siteId|$username" // bind ciphertext to context (Aad)
        val cipherB64 = crypto.encryptToBase64(plainPassword, aad)
        dao.insertCred(
            CredentialEntity(
                siteId = siteId,
                username = username,
                passwordCipherB64 = cipherB64,
                lastUpdated = lastUpdated
            )
        )
    }

    override suspend fun deleteSite(siteId: Long) {
        // Fetch site, then delete (CASCADE delete credentials)
        val temp = SiteEntity(id = siteId, site = "", note = null)
        dao.deleteSite(temp)
    }

    override suspend fun deleteCredential(siteId: Long, username: String) {
        dao.deleteCredential(siteId, username)
    }
}

private fun SiteWithCreds.toDomain(crypto: CryptoManager): SiteAccount {
    val creds = creds.map { e ->
        val aad = "${site.id}|${e.username}"
        val plain = runCatching {
            crypto.decryptFromBase64(
                e.passwordCipherB64,
                aad
            )
        }.getOrDefault("••••••••")
        UserCredential(
            username = e.username,
            password = plain,       // ⚠️ Only display for demonstration purposes; do not expose plaintext in the actual UI!
            lastUpdated = e.lastUpdated
        )
    }
    return SiteAccount(
        id = site.id,
        site = site.site,
        note = site.note,
        credentials = creds
    )
}
