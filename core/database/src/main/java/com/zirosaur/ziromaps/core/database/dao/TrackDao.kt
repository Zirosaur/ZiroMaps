package com.zirosaur.ziromaps.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.TrackPointEntity
import com.zirosaur.ziromaps.core.database.model.TrackWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getTrackById(id: String): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackByIdSync(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY startedAtEpochMs DESC")
    fun getTracksForProject(projectId: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY recordedAtEpochMs ASC")
    fun getTrackPoints(trackId: String): Flow<List<TrackPointEntity>>

    @Transaction
    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getTrackWithPoints(id: String): Flow<TrackWithPoints?>

    @Transaction
    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY startedAtEpochMs DESC")
    fun getTracksWithPointsForProject(projectId: String): Flow<List<TrackWithPoints>>
}
