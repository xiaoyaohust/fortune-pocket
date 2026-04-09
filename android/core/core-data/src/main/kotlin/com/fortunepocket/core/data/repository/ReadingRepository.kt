package com.fortunepocket.core.data.repository

import com.fortunepocket.core.data.db.ReadingRecordDao
import com.fortunepocket.core.data.db.toDomain
import com.fortunepocket.core.data.db.toEntity
import com.fortunepocket.core.model.ReadingRecord
import com.fortunepocket.core.model.ReadingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRepository @Inject constructor(
    private val dao: ReadingRecordDao
) {

    fun observeAll(): Flow<List<ReadingRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByType(type: ReadingType): Flow<List<ReadingRecord>> =
        dao.observeByType(type.key).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): ReadingRecord? =
        dao.getById(id)?.toDomain()

    suspend fun save(record: ReadingRecord) =
        dao.insert(record.toEntity())

    suspend fun delete(id: String) =
        dao.deleteById(id)

    suspend fun clearAll() =
        dao.deleteAll()
}
