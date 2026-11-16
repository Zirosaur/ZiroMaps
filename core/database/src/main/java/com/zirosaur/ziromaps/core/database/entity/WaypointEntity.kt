package com.zirosaur.ziromaps.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = MapProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"])
    ]
)
data class WaypointEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val title: String,
    val note: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
    val accuracyM: Float?,
    val hasPhoto: Boolean,
    val createdAtEpochMs: Long
)
