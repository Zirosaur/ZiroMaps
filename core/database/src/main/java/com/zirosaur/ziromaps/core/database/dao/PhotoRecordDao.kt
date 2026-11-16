package com.zirosaur.ziromaps.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zirosaur.ziromaps.core.database.entity.PhotoRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoRecordEntity>)

    @Update
    suspend fun update(photo: PhotoRecordEntity)

    @Delete
    suspend fun delete(photo: PhotoRecordEntity)

    @Query("DELETE FROM photo_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM photo_records WHERE id = :id")
    fun getPhotoById(id: String): Flow<PhotoRecordEntity?>

    @Query("SELECT * FROM photo_records WHERE id = :id")
    suspend fun getPhotoByIdSync(id: String): PhotoRecordEntity?

    @Query("SELECT * FROM photo_records WHERE waypointId = :waypointId ORDER BY capturedAtEpochMs ASC")
    fun getPhotosForWaypoint(waypointId: String): Flow<List<PhotoRecordEntity>>
}
