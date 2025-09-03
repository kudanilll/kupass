package com.nielcode.kupass.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "credentials",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("siteId")]
)
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteId: Long,
    val username: String,
    val passwordCipherB64: String,   // dienkripsi (Tink AES-GCM)
    val lastUpdated: Date
)
