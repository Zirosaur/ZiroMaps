package com.zirosaur.ziromaps.ui.camera

import com.zirosaur.ziromaps.core.camera.model.CameraState
import com.zirosaur.ziromaps.core.camera.model.FlashMode
import com.zirosaur.ziromaps.core.camera.model.LensFacing
import com.zirosaur.ziromaps.core.location.model.CompassData
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.model.UtmCoordinate

data class CameraStampUiState(
    val projectId: String = "",
    val projectName: String = "",
    val cameraPermissionGranted: Boolean = false,
    val cameraState: CameraState = CameraState.Uninitialized,
    val flashMode: FlashMode = FlashMode.OFF,
    val lensFacing: LensFacing = LensFacing.BACK,
    val zoomRatio: Float = 1f,
    val currentLocation: UserLocation? = null,
    val currentUtm: UtmCoordinate? = null,
    val currentCompass: CompassData? = null,
    val quickNote: String = "",
    val isSavingPhoto: Boolean = false,
    val photoSavedWaypointId: String? = null,
    val errorMessage: String? = null,
    val userNotice: String? = null
) {
    val isGpsFixed: Boolean
        get() = currentLocation != null

    val isHighAccuracy: Boolean
        get() = currentLocation?.accuracyM?.let { it <= 30f } ?: false
}
