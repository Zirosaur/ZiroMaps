package com.zirosaur.ziromaps.ui.viewer

import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.model.TrackWithPoints
import com.zirosaur.ziromaps.core.database.model.WaypointWithPhotos
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.location.model.CompassData
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.model.UtmCoordinate

/**
 * Immutable UI state for MapViewerScreen.
 */
data class MapViewerUiState(
    val isLoading: Boolean = true,
    val mapProject: MapProjectEntity? = null,
    val transformMatrix: TransformMatrix? = null,
    val currentLocation: UserLocation? = null,
    val currentCompass: CompassData? = null,
    val currentUtm: UtmCoordinate? = null,
    val userPagePoint: PagePoint? = null,
    val coordinateFormat: CoordinateFormat = CoordinateFormat.UTM,
    val isGpsInsideBounds: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val centerGpsTrigger: Long = 0L,
    val waypoints: List<WaypointWithPhotos> = emptyList(),
    val tracks: List<TrackWithPoints> = emptyList(),
    val isRecordingTrack: Boolean = false,
    val recordedDistanceM: Double = 0.0,
    val showAddWaypointDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val isExporting: Boolean = false,
    val selectedWaypoint: WaypointWithPhotos? = null,
    val mapCenterLatLon: LatLon? = null,
    val errorMessage: String? = null,
    val userNotice: String? = null
)
