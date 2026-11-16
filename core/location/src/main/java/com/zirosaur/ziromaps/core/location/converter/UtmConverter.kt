package com.zirosaur.ziromaps.core.location.converter

import com.zirosaur.ziromaps.core.location.model.UtmCoordinate
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * High-precision WGS84 <-> UTM (Universal Transverse Mercator) Coordinate Converter.
 * Based on Karney / Snyder formulations for the WGS84 ellipsoid.
 */
object UtmConverter {

    // WGS84 Ellipsoid constants
    private const val A = 6378137.0 // Semi-major axis (meters)
    private const val F = 1.0 / 298.257223563 // Flattening
    private const val E2 = 2.0 * F - F * F // First eccentricity squared
    private const val E_PRIME2 = E2 / (1.0 - E2) // Second eccentricity squared
    private const val K0 = 0.9996 // Central meridian scale factor

    // Precalculated expansion coefficients
    private val E1 = (1.0 - sqrt(1.0 - E2)) / (1.0 + sqrt(1.0 - E2))
    private val M_COEF = A * (1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0)

    private const val LATITUDE_BANDS = "CDEFGHJKLMNPQRSTUVWX"

    /**
     * Determines the UTM longitudinal zone (1 to 60) for a given longitude.
     */
    fun getZone(longitude: Double): Int {
        var lon = longitude
        while (lon < -180.0) lon += 360.0
        while (lon >= 180.0) lon -= 360.0
        return floor((lon + 180.0) / 6.0).toInt() + 1
    }

    /**
     * Determines the UTM latitudinal band character ('C' to 'X') for a given latitude.
     */
    fun getBand(latitude: Double): Char {
        if (latitude < -80.0 || latitude > 84.0) {
            return if (latitude < 0) 'C' else 'X'
        }
        val idx = ((latitude + 80.0) / 8.0).toInt().coerceIn(0, LATITUDE_BANDS.length - 1)
        return LATITUDE_BANDS[idx]
    }

    /**
     * Converts WGS84 Latitude and Longitude to UTM Coordinate.
     *
     * @param latitude in decimal degrees (-80.0 to 84.0)
     * @param longitude in decimal degrees (-180.0 to 180.0)
     * @return [UtmCoordinate] containing zone, band, easting, northing, and hemisphere.
     */
    fun latLonToUtm(latitude: Double, longitude: Double): UtmCoordinate {
        val zone = getZone(longitude)
        val band = getBand(latitude)
        val isSouth = latitude < 0.0

        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)
        val lon0Rad = Math.toRadians(((zone - 1) * 6 - 180 + 3).toDouble())

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val tanLat = tan(latRad)

        val n = A / sqrt(1.0 - E2 * sinLat * sinLat)
        val t = tanLat * tanLat
        val c = E_PRIME2 * cosLat * cosLat
        val aDelta = cosLat * (lonRad - lon0Rad)

        val m = A * (
                (1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0) * latRad
                        - (3.0 * E2 / 8.0 + 3.0 * E2 * E2 / 32.0 + 45.0 * E2 * E2 * E2 / 1024.0) * sin(2.0 * latRad)
                        + (15.0 * E2 * E2 / 256.0 + 45.0 * E2 * E2 * E2 / 1024.0) * sin(4.0 * latRad)
                        - (35.0 * E2 * E2 * E2 / 3072.0) * sin(6.0 * latRad)
                )

        val a2 = aDelta * aDelta
        val a3 = a2 * aDelta
        val a4 = a2 * a2
        val a5 = a4 * aDelta

        var easting = K0 * n * (
                aDelta
                        + (1.0 - t + c) * a3 / 6.0
                        + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * E_PRIME2) * a5 / 120.0
                ) + 500000.0

        var northing = K0 * (
                m + n * tanLat * (
                        a2 / 2.0
                                + (5.0 - t + 9.0 * c + 4.0 * c * c) * a4 / 24.0
                                + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * E_PRIME2) * (a4 * a2) / 720.0
                        )
                )

        if (isSouth) {
            northing += 10000000.0
        }

        return UtmCoordinate(
            zone = zone,
            band = band,
            eastingM = easting,
            northingM = northing,
            isSouth = isSouth
        )
    }

    /**
     * Converts UTM Coordinate back to WGS84 Latitude and Longitude.
     *
     * @param zone UTM zone (1 to 60)
     * @param eastingM Easting in meters
     * @param northingM Northing in meters
     * @param isSouth true if located in the southern hemisphere
     * @return [Pair] of (Latitude, Longitude) in decimal degrees.
     */
    fun utmToLatLon(
        zone: Int,
        eastingM: Double,
        northingM: Double,
        isSouth: Boolean
    ): Pair<Double, Double> {
        val x = eastingM - 500000.0
        var y = northingM
        if (isSouth) {
            y -= 10000000.0
        }

        val m = y / K0
        val mu = m / M_COEF

        val fp = mu + (3.0 * E1 / 2.0 - 27.0 * E1 * E1 * E1 / 32.0) * sin(2.0 * mu) +
                (21.0 * E1 * E1 / 16.0 - 55.0 * E1 * E1 * E1 * E1 / 32.0) * sin(4.0 * mu) +
                (151.0 * E1 * E1 * E1 / 96.0) * sin(6.0 * mu) +
                (1097.0 * E1 * E1 * E1 * E1 / 512.0) * sin(8.0 * mu)

        val sinFp = sin(fp)
        val cosFp = cos(fp)
        val tanFp = tan(fp)

        val c1 = E_PRIME2 * cosFp * cosFp
        val t1 = tanFp * tanFp
        val n1 = A / sqrt(1.0 - E2 * sinFp * sinFp)
        val r1 = A * (1.0 - E2) / ((1.0 - E2 * sinFp * sinFp) * sqrt(1.0 - E2 * sinFp * sinFp))
        val d = x / (n1 * K0)

        val d2 = d * d
        val d3 = d2 * d
        val d4 = d2 * d2
        val d5 = d4 * d
        val d6 = d4 * d2

        val latRad = fp - (n1 * tanFp / r1) * (
                d2 / 2.0
                        - (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * E_PRIME2) * d4 / 24.0
                        + (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * E_PRIME2 - 3.0 * c1 * c1) * d6 / 720.0
                )

        val lon0 = ((zone - 1) * 6 - 180 + 3).toDouble()
        val lonRad = (d - (1.0 + 2.0 * t1 + c1) * d3 / 6.0
                + (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * E_PRIME2 + 24.0 * t1 * t1) * d5 / 120.0) / cosFp

        val latitude = Math.toDegrees(latRad)
        val longitude = lon0 + Math.toDegrees(lonRad)

        return Pair(latitude, longitude)
    }

    fun utmToLatLon(utm: UtmCoordinate): Pair<Double, Double> {
        return utmToLatLon(utm.zone, utm.eastingM, utm.northingM, utm.isSouth)
    }
}
