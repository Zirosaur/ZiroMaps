package com.zirosaur.ziromaps.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.zirosaur.ziromaps.core.database.entity.PhotoRecordEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity

data class WaypointWithPhotos(
    @Embedded val waypoint: WaypointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "waypointId"
    )
    val photos: List<PhotoRecordEntity>
)
