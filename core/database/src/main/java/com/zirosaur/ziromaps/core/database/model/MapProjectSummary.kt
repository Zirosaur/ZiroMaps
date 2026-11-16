package com.zirosaur.ziromaps.core.database.model

import androidx.room.Embedded
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity

/**
 * Summary model for a map project with aggregated counts of waypoints and photos.
 */
data class MapProjectSummary(
    @Embedded val project: MapProjectEntity,
    val waypointCount: Int = 0,
    val photoCount: Int = 0
)
