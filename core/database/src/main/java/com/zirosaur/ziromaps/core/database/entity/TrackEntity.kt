package com.zirosaur.ziromaps.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
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
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val name: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val totalDistanceM: Double
)
