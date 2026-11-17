package com.zirosaur.ziromaps.model

/**
 * UI model representing a GeoPDF map project in the library.
 */
data class MapProjectItem(
    val id: String,
    val name: String,
    val pdfLocalPath: String,
    val pageWidthPt: Double,
    val pageHeightPt: Double,
    val geoStandard: String,
    val epsgCode: Int?,
    val waypointCount: Int,
    val photoCount: Int,
    val trackCount: Int = 0,
    val fileSizeBytes: Long = 0L,
    val formattedFileSize: String = "",
    val createdAtEpochMs: Long,
    val formattedDate: String
)
