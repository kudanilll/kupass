package com.nielcode.kupass.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for password CRUD operations. Reactive reads return Flow for UI updates.
 */
@Dao
interface PasswordDao {

    /**
     * Every row, unordered. Text columns hold ciphertext, so SQL ordering and `LIKE` search are
     * meaningless; the repository sorts and searches after decryption.
     */
    @Query("SELECT * FROM passwords")
    fun getAll(): Flow<List<PasswordEntity>>

    /** One-shot snapshot of every row, used by the storage-format upgrade. */
    @Query("SELECT * FROM passwords")
    suspend fun getAllOnce(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE id = :id") fun getById(id: Long): Flow<PasswordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: PasswordEntity): Long

    /** Inserts all rows in one transaction: either every row is stored or none is. */
    @Insert suspend fun insertAll(passwords: List<PasswordEntity>): List<Long>

    @Update suspend fun update(password: PasswordEntity)

    /** Updates all rows in one transaction. */
    @Update suspend fun updateAll(passwords: List<PasswordEntity>)

    @Delete suspend fun delete(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id") suspend fun deleteById(id: Long)
}
