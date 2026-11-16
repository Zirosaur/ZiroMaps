package com.zirosaur.ziromaps.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.TrackPointEntity

data class TrackWithPoints(
    @Embedded val track: TrackEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val points: List<TrackPointEntity>
)
