package com.zirosaur.ziromaps.core.location.model

import java.util.Locale

/**
 * Universal Transverse Mercator (UTM) coordinate representation.
 *
 * @param zone UTM longitudinal zone (1 to 60)
 * @param band UTM latitudinal 8-degree band letter ('C' to 'X', excluding 'I' and 'O')
 * @param eastingM Easting coordinate in meters
 * @param northingM Northing coordinate in meters
 * @param isSouth true if located in the Southern hemisphere
 */
data class UtmCoordinate(
    val zone: Int,
    val band: Char,
    val eastingM: Double,
    val northingM: Double,
    val isSouth: Boolean
) {
    /**
     * Formats the UTM coordinate as standard SSOT string:
     * `<Zone><Band> <Easting>m E <Northing>m N`
     * Example: `49M 305120m E 9178110m N`
     */
    fun toFormattedString(): String {
        val eastingInt = eastingM.toLong()
        val northingInt = northingM.toLong()
        return String.format(Locale.US, "%d%c %dm E %dm N", zone, band, eastingInt, northingInt)
    }
}
