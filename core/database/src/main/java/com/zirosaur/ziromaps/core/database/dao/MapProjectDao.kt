package com.zirosaur.ziromaps.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.model.MapProjectWithWaypoints
import kotlinx.coroutines.flow.Flow

@Dao
interface MapProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: MapProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<MapProjectEntity>)

    @Update
    suspend fun update(project: MapProjectEntity)

    @Delete
    suspend fun delete(project: MapProjectEntity)

    @Query("DELETE FROM map_projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM map_projects WHERE id = :id")
    fun getProjectById(id: String): Flow<MapProjectEntity?>

    @Query("SELECT * FROM map_projects WHERE id = :id")
    suspend fun getProjectByIdSync(id: String): MapProjectEntity?

    @Query("SELECT * FROM map_projects ORDER BY createdAtEpochMs DESC")
    fun getAllProjects(): Flow<List<MapProjectEntity>>

    @Transaction
    @Query("SELECT * FROM map_projects WHERE id = :id")
    fun getProjectWithWaypoints(id: String): Flow<MapProjectWithWaypoints?>
}
