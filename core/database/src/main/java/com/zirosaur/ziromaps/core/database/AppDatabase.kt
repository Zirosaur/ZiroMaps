package com.zirosaur.ziromaps.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.PhotoRecordDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.PhotoRecordEntity
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.TrackPointEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity

@Database(
    entities = [
        MapProjectEntity::class,
        WaypointEntity::class,
        PhotoRecordEntity::class,
        TrackEntity::class,
        TrackPointEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mapProjectDao(): MapProjectDao
    abstract fun waypointDao(): WaypointDao
    abstract fun photoRecordDao(): PhotoRecordDao
    abstract fun trackDao(): TrackDao

    companion object {
        const val DATABASE_NAME = "ziro_maps.db"
    }
}
