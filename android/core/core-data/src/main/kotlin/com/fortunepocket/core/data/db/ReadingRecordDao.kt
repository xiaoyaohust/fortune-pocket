package com.fortunepocket.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingRecordDao {

    @Query("SELECT * FROM reading_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReadingRecordEntity>>

    @Query("SELECT * FROM reading_records WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<ReadingRecordEntity>>

    @Query("SELECT * FROM reading_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReadingRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ReadingRecordEntity)

    @Query("DELETE FROM reading_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reading_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM reading_records")
    suspend fun count(): Int
}
