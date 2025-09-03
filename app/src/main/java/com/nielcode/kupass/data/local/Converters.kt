package com.nielcode.kupass.data.local

import androidx.room.TypeConverter
import java.util.*

class Converters {
    @TypeConverter
    fun fromDate(d: Date?): Long? = d?.time
    @TypeConverter
    fun toDate(v: Long?): Date? = v?.let { Date(it) }
}
