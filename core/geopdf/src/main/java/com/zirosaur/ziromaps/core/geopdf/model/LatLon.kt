package com.zirosaur.ziromaps.core.geopdf.model

/**
 * Represents a geographic coordinate in WGS84 (EPSG:4326).
 * Latitude ranges from -90 to 90 degrees (positive North, negative South).
 * Longitude ranges from -180 to 180 degrees (positive East, negative West).
 */
data class LatLon(
    val latitude: Double,
    val longitude: Double
)
