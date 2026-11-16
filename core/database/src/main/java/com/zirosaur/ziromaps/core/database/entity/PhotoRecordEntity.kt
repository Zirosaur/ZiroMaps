package com.zirosaur.ziromaps.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_records",
    foreignKeys = [
        ForeignKey(
            entity = WaypointEntity::class,
            parentColumns = ["id"],
            childColumns = ["waypointId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["waypointId"])
    ]
)
data class PhotoRecordEntity(
    @PrimaryKey
    val id: String,
    val waypointId: String,
    val filePath: String,
    val azimuthDeg: Float?,
    val capturedAtEpochMs: Long
)
