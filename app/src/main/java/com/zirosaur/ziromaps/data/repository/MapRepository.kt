package com.zirosaur.ziromaps.data.repository

import android.net.Uri
import com.zirosaur.ziromaps.model.MapProjectItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing offline GeoPDF map projects.
 */
interface MapRepository {

    /**
     * Observes all imported map projects ordered by creation date descending.
     */
    fun getMapProjects(): Flow<List<MapProjectItem>>

    /**
     * Imports and validates a GeoPDF document from a SAF [Uri].
     *
     * @param uri the content/file URI selected by the user.
     * @return [Result] containing the imported [MapProjectItem] on success,
     * or [com.zirosaur.ziromaps.domain.NotGeoPdfException] / [Exception] on failure.
     */
    suspend fun importMapFromUri(uri: Uri): Result<MapProjectItem>

    /**
     * Deletes a map project and all its associated waypoints, photos, and files.
     *
     * @param projectId unique identifier of the map project.
     */
    suspend fun deleteMapProject(projectId: String)
}
