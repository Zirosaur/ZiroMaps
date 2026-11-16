package com.zirosaur.ziromaps.core.location.model

/**
 * Immutable snapshot of the user's geographic location.
 *
 * @param latitude WGS84 latitude in decimal degrees (-90.0 to 90.0)
 * @param longitude WGS84 longitude in decimal degrees (-180.0 to 180.0)
 * @param altitudeM altitude above WGS84 ellipsoid in meters (optional)
 * @param accuracyM estimated horizontal 1-sigma radius accuracy in meters (optional)
 * @param speedMps current ground speed in meters per second (optional)
 * @param bearingDeg current ground track bearing in degrees (0.0 to 360.0, optional)
 * @param timestampEpochMs time when this fix was generated in epoch milliseconds
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val accuracyM: Float? = null,
    val speedMps: Float? = null,
    val bearingDeg: Float? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
