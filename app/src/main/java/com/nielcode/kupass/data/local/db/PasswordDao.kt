package com.nielcode.kupass.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for password CRUD operations.
 * All read operations return Flow for reactive UI updates.
 */
@Dao
interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY site_name ASC")
    fun getAll(): Flow<List<PasswordEntity>>

    @Query(
        "SELECT * FROM passwords WHERE site_name LIKE '%' || :query || '%' " +
                "OR username LIKE '%' || :query || '%' " +
                "ORDER BY site_name ASC"
    )
    fun search(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id = :id")
    fun getById(id: Long): Flow<PasswordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: PasswordEntity): Long

    @Update
    suspend fun update(password: PasswordEntity)

    @Delete
    suspend fun delete(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deleteById(id: Long)
}
