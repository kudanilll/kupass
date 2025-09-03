package com.nielcode.kupass.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,
    val note: String?
)
