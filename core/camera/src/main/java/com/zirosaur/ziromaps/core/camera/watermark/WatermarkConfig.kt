package com.zirosaur.ziromaps.core.camera.watermark

enum class WatermarkPosition(val displayName: String) {
    BOTTOM_LEFT("Kiri Bawah"),
    BOTTOM_RIGHT("Kanan Bawah"),
    TOP_LEFT("Kiri Atas")
}

enum class WatermarkTextSize(val displayName: String, val scaleFactor: Float) {
    SMALL("Kecil", 0.8f),
    MEDIUM("Sedang", 1.0f),
    LARGE("Besar", 1.3f)
}

data class WatermarkConfig(
    val showLatLon: Boolean = true,
    val showUtm: Boolean = true,
    val showAzimuth: Boolean = true,
    val showAltitude: Boolean = true,
    val showTimestamp: Boolean = true,
    val showMapName: Boolean = true,
    val showNote: Boolean = true,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val textSize: WatermarkTextSize = WatermarkTextSize.MEDIUM
) {
    val showCoordinates: Boolean
        get() = showLatLon
}
