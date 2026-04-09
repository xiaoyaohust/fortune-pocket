package com.fortunepocket.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReadingRecordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingRecordDao(): ReadingRecordDao

    companion object {
        const val DATABASE_NAME = "fortune_pocket.db"
    }
}
