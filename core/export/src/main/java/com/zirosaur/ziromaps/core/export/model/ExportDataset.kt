package com.zirosaur.ziromaps.core.export.model

data class ExportWaypoint(
    val id: String,
    val title: String,
    val note: String = "",
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val accuracyM: Float? = null,
    val hasPhoto: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val photoFilePath: String? = null,
    val photoAzimuthDeg: Float? = null
)

data class ExportTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val accuracyM: Float? = null,
    val recordedAtEpochMs: Long = System.currentTimeMillis()
)

data class ExportTrack(
    val id: String,
    val name: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val totalDistanceM: Double = 0.0,
    val points: List<ExportTrackPoint> = emptyList()
)

data class ExportDataset(
    val projectName: String,
    val waypoints: List<ExportWaypoint> = emptyList(),
    val tracks: List<ExportTrack> = emptyList()
)
