package com.zirosaur.ziromaps.core.camera.watermark

import java.util.TimeZone

data class WatermarkData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeM: Double? = null,
    val accuracyM: Float? = null,
    val utmCoordinate: String? = null,
    val azimuthDeg: Float? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val timeZone: TimeZone = TimeZone.getDefault(),
    val mapProjectName: String? = null,
    val note: String? = null,
    val config: WatermarkConfig = WatermarkConfig()
) {
    val isGpsFixed: Boolean
        get() = latitude != null && longitude != null

    val isHighAccuracy: Boolean
        get() = accuracyM != null && accuracyM <= 30f
}
