package com.zirosaur.ziromaps.core.location.model

import java.util.Locale

/**
 * Immutable azimuth reading from device orientation sensors.
 *
 * @param azimuthDeg azimuth angle in degrees clockwise from True North (0.0 to 360.0)
 * @param cardinalDirection cardinal / intercardinal abbreviation ("N", "NE", "E", "SE", "S", "SW", "W", "NW")
 */
data class CompassData(
    val azimuthDeg: Float,
    val cardinalDirection: String = getCardinal(azimuthDeg)
) {
    /**
     * Formats azimuth into standard format, e.g. `142° SE`.
     */
    fun toFormattedString(): String {
        return String.format(Locale.US, "%d° %s", azimuthDeg.toInt(), cardinalDirection)
    }

    companion object {
        fun getCardinal(azimuth: Float): String {
            val normalized = (azimuth % 360f + 360f) % 360f
            return when {
                normalized >= 337.5f || normalized < 22.5f -> "N"
                normalized < 67.5f -> "NE"
                normalized < 112.5f -> "E"
                normalized < 157.5f -> "SE"
                normalized < 202.5f -> "S"
                normalized < 247.5f -> "SW"
                normalized < 292.5f -> "W"
                else -> "NW"
            }
        }
    }
}
