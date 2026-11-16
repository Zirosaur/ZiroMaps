package com.zirosaur.ziromaps.core.camera.watermark

object CompassDirection {

    private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    fun toCardinal(azimuthDeg: Float): String {
        val normalized = (azimuthDeg % 360f + 360f) % 360f
        val index = (((normalized + 22.5f) % 360f) / 45f).toInt()
        return CARDINALS[index.coerceIn(0, 7)]
    }

    fun formatAzimuth(azimuthDeg: Float?): String {
        if (azimuthDeg == null) return "-"
        val normalized = (azimuthDeg % 360f + 360f) % 360f
        val cardinal = toCardinal(normalized)
        return "${normalized.toInt()}° $cardinal"
    }
}
