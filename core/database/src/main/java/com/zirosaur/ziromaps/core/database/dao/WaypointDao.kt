package com.zirosaur.ziromaps.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import com.zirosaur.ziromaps.core.database.model.WaypointWithPhotos
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: WaypointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waypoints: List<WaypointEntity>)

    @Update
    suspend fun update(waypoint: WaypointEntity)

    @Delete
    suspend fun delete(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM waypoints WHERE id = :id")
    fun getWaypointById(id: String): Flow<WaypointEntity?>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getWaypointByIdSync(id: String): WaypointEntity?

    @Query("SELECT * FROM waypoints WHERE projectId = :projectId ORDER BY createdAtEpochMs ASC")
    fun getWaypointsForProject(projectId: String): Flow<List<WaypointEntity>>

    @Query("SELECT COUNT(*) FROM waypoints WHERE projectId = :projectId")
    suspend fun getWaypointCountForProject(projectId: String): Int

    @Transaction
    @Query("SELECT * FROM waypoints WHERE id = :id")
    fun getWaypointWithPhotos(id: String): Flow<WaypointWithPhotos?>

    @Transaction
    @Query("SELECT * FROM waypoints WHERE projectId = :projectId ORDER BY createdAtEpochMs ASC")
    fun getWaypointsWithPhotosForProject(projectId: String): Flow<List<WaypointWithPhotos>>
}
