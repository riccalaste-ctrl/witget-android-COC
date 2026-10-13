package com.zacatac.clashboard.di

import android.content.Context
import androidx.room.Room
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.parser.CoCExportParser
import com.zacatac.clashboard.data.repository.ProfileRepository
import com.zacatac.clashboard.data.repository.UpgradeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "clashboard_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProfileRepository(database: AppDatabase): ProfileRepository {
        return ProfileRepository(database)
    }

    @Provides
    @Singleton
    fun provideUpgradeRepository(database: AppDatabase): UpgradeRepository {
        return UpgradeRepository(database)
    }

    @Provides
    @Singleton
    fun provideExportParser(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): CoCExportParser {
        return CoCExportParser(context)
    }
}