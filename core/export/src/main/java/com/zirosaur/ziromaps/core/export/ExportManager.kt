package com.zirosaur.ziromaps.core.export

import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.export.csv.CsvExporter
import com.zirosaur.ziromaps.core.export.geojson.GeoJsonExporter
import com.zirosaur.ziromaps.core.export.kml.KmzExporter
import com.zirosaur.ziromaps.core.export.model.ExportDataset
import com.zirosaur.ziromaps.core.export.model.ExportFormat
import com.zirosaur.ziromaps.core.export.model.ExportTrack
import com.zirosaur.ziromaps.core.export.model.ExportTrackPoint
import com.zirosaur.ziromaps.core.export.model.ExportWaypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ExportManager {
    suspend fun exportDataset(
        dataset: ExportDataset,
        format: ExportFormat,
        destinationFile: File
    ): Result<File>

    suspend fun exportProject(
        projectId: String,
        format: ExportFormat,
        destinationDir: File
    ): Result<File>
}

@Singleton
class ExportManagerImpl @Inject constructor(
    private val geoJsonExporter: GeoJsonExporter,
    private val csvExporter: CsvExporter,
    private val kmzExporter: KmzExporter,
    private val mapProjectDao: MapProjectDao,
    private val waypointDao: WaypointDao,
    private val trackDao: TrackDao
) : ExportManager {

    override suspend fun exportDataset(
        dataset: ExportDataset,
        format: ExportFormat,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = when (format) {
                ExportFormat.GEOJSON -> geoJsonExporter.exportToGeoJson(dataset, destinationFile)
                ExportFormat.CSV -> csvExporter.exportToCsv(dataset, destinationFile)
                ExportFormat.KMZ -> kmzExporter.exportToKmz(dataset, destinationFile)
            }
            Result.success(file)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun exportProject(
        projectId: String,
        format: ExportFormat,
        destinationDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val project = mapProjectDao.getProjectByIdSync(projectId)
                ?: return@withContext Result.failure(IllegalArgumentException("Proyek tidak ditemukan: $projectId"))

            val waypointsWithPhotos = waypointDao.getWaypointsWithPhotosForProject(projectId).first()
            val tracksWithPoints = trackDao.getTracksWithPointsForProject(projectId).first()

            val exportWaypoints = waypointsWithPhotos.map { wpWithPhotos ->
                val wp = wpWithPhotos.waypoint
                val primaryPhoto = wpWithPhotos.photos.firstOrNull()
                ExportWaypoint(
                    id = wp.id,
                    title = wp.title,
                    note = wp.note,
                    latitude = wp.latitude,
                    longitude = wp.longitude,
                    altitudeM = wp.altitudeM,
                    accuracyM = wp.accuracyM,
                    hasPhoto = wp.hasPhoto,
                    createdAtEpochMs = wp.createdAtEpochMs,
                    photoFilePath = primaryPhoto?.filePath,
                    photoAzimuthDeg = primaryPhoto?.azimuthDeg
                )
            }

            val exportTracks = tracksWithPoints.map { trackWithPts ->
                val track = trackWithPts.track
                val points = trackWithPts.points.map { pt ->
                    ExportTrackPoint(
                        latitude = pt.latitude,
                        longitude = pt.longitude,
                        altitudeM = pt.altitudeM,
                        accuracyM = pt.accuracyM,
                        recordedAtEpochMs = pt.recordedAtEpochMs
                    )
                }
                ExportTrack(
                    id = track.id,
                    name = track.name,
                    startedAtEpochMs = track.startedAtEpochMs,
                    endedAtEpochMs = track.endedAtEpochMs,
                    totalDistanceM = track.totalDistanceM,
                    points = points
                )
            }

            val dataset = ExportDataset(
                projectName = project.name,
                waypoints = exportWaypoints,
                tracks = exportTracks
            )

            val safeProjectName = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${safeProjectName}_export.${format.fileExtension}"
            val destinationFile = File(destinationDir, fileName)

            exportDataset(dataset, format, destinationFile)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
