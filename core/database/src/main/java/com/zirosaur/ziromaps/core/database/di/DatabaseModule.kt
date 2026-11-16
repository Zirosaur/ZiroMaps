package com.zirosaur.ziromaps.core.database.di

import android.content.Context
import androidx.room.Room
import com.zirosaur.ziromaps.core.database.AppDatabase
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.PhotoRecordDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideMapProjectDao(database: AppDatabase): MapProjectDao = database.mapProjectDao()

    @Provides
    fun provideWaypointDao(database: AppDatabase): WaypointDao = database.waypointDao()

    @Provides
    fun providePhotoRecordDao(database: AppDatabase): PhotoRecordDao = database.photoRecordDao()

    @Provides
    fun provideTrackDao(database: AppDatabase): TrackDao = database.trackDao()
}
