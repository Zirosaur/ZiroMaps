package com.zirosaur.ziromaps.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity

data class MapProjectWithWaypoints(
    @Embedded val project: MapProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val waypoints: List<WaypointEntity>
)
