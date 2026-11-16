package com.zirosaur.ziromaps.core.location.converter

import java.util.Locale
import kotlin.math.abs

/**
 * Utility for formatting coordinates for map UI panels and camera watermark stamps.
 */
object CoordinateFormatter {

    /**
     * Formats latitude and longitude into decimal degrees string.
     * Example: `Lat: -7.431250°  Lon: 109.234510°`
     */
    fun toDecimalString(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "Lat: %.6f°  Lon: %.6f°", latitude, longitude)
    }

    /**
     * Formats latitude and longitude into DMS (Degrees, Minutes, Seconds) representation.
     * Example: `7°25'52.5"S  109°14'04.2"E`
     */
    fun toDmsString(latitude: Double, longitude: Double): String {
        val latDms = formatSingleDms(latitude, isLatitude = true)
        val lonDms = formatSingleDms(longitude, isLatitude = false)
        return "$latDms  $lonDms"
    }

    private fun formatSingleDms(value: Double, isLatitude: Boolean): String {
        val positive = abs(value)
        val degrees = positive.toInt()
        val minutesDecimal = (positive - degrees) * 60.0
        val minutes = minutesDecimal.toInt()
        val seconds = (minutesDecimal - minutes) * 60.0

        val hemisphere = if (isLatitude) {
            if (value >= 0.0) "N" else "S"
        } else {
            if (value >= 0.0) "E" else "W"
        }

        return String.format(Locale.US, "%d°%02d'%04.1f\"%s", degrees, minutes, seconds, hemisphere)
    }

    /**
     * Converts coordinate to UTM and returns formatted string.
     * Example: `49M 305120m E 9178110m N`
     */
    fun toUtmString(latitude: Double, longitude: Double): String {
        val utm = UtmConverter.latLonToUtm(latitude, longitude)
        return utm.toFormattedString()
    }
}
