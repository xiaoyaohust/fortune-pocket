package com.fortunepocket.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fortunepocket.core.model.ReadingRecord
import com.fortunepocket.core.model.ReadingType

@Entity(tableName = "reading_records")
data class ReadingRecordEntity(
    @PrimaryKey val id: String,
    val type: String,           // ReadingType.key
    val createdAt: Long,        // epoch millis
    val title: String,
    val summary: String,
    val detailJson: String,
    val schemaVersion: Int = 1,
    val isPremium: Boolean = false
)

fun ReadingRecordEntity.toDomain(): ReadingRecord = ReadingRecord(
    id = id,
    type = ReadingType.fromKey(type),
    createdAt = createdAt,
    title = title,
    summary = summary,
    detailJson = detailJson,
    schemaVersion = schemaVersion,
    isPremium = isPremium
)

fun ReadingRecord.toEntity(): ReadingRecordEntity = ReadingRecordEntity(
    id = id,
    type = type.key,
    createdAt = createdAt,
    title = title,
    summary = summary,
    detailJson = detailJson,
    schemaVersion = schemaVersion,
    isPremium = isPremium
)
