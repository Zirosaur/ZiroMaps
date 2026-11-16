package com.zirosaur.ziromaps.core.camera.model

import android.graphics.Bitmap
import android.view.Surface
import androidx.camera.core.ImageCapture

enum class LensFacing {
    BACK,
    FRONT
}

enum class FlashMode {
    OFF,
    ON,
    AUTO;

    fun toCameraXFlashMode(): Int = when (this) {
        OFF -> ImageCapture.FLASH_MODE_OFF
        ON -> ImageCapture.FLASH_MODE_ON
        AUTO -> ImageCapture.FLASH_MODE_AUTO
    }
}

enum class DeviceOrientation(val degrees: Int, val surfaceRotation: Int) {
    PORTRAIT_UP(0, Surface.ROTATION_0),
    LANDSCAPE_RIGHT(90, Surface.ROTATION_90),
    PORTRAIT_DOWN(180, Surface.ROTATION_180),
    LANDSCAPE_LEFT(270, Surface.ROTATION_270);

    companion object {
        fun fromDegrees(deg: Int): DeviceOrientation {
            val normalized = (deg % 360 + 360) % 360
            return when {
                normalized in 45..134 -> LANDSCAPE_RIGHT
                normalized in 135..224 -> PORTRAIT_DOWN
                normalized in 225..314 -> LANDSCAPE_LEFT
                else -> PORTRAIT_UP
            }
        }
    }
}

data class CapturedImage(
    val bitmap: Bitmap,
    val rotationDegrees: Int,
    val width: Int,
    val height: Int,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isMirrored: Boolean = false
)

sealed interface CameraState {
    data object Uninitialized : CameraState
    data object Starting : CameraState
    data object Ready : CameraState
    data object Capturing : CameraState
    data class Error(val message: String, val cause: Throwable? = null) : CameraState
}
