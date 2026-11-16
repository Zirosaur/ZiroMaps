package com.zirosaur.ziromaps.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_projects")
data class MapProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val pdfLocalPath: String,
    val pageWidthPt: Double,
    val pageHeightPt: Double,
    val geoStandard: String,
    val epsgCode: Int?,
    val transformMatrixJson: String,
    val createdAtEpochMs: Long
)
