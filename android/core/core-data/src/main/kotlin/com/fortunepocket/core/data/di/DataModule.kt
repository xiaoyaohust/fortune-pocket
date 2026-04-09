package com.fortunepocket.core.data.di

import android.content.Context
import androidx.room.Room
import com.fortunepocket.core.data.db.AppDatabase
import com.fortunepocket.core.data.db.ReadingRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideReadingRecordDao(db: AppDatabase): ReadingRecordDao = db.readingRecordDao()
}
