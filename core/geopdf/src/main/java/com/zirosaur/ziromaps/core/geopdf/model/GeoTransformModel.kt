package com.zirosaur.ziromaps.core.geopdf.model

/**
 * Deterministic domain model for a georeferenced PDF page.
 * Provides coordinate transformations between PDF page points (1/72 inch user space)
 * and WGS84 geographic coordinates (latitude, longitude).
 */
data class GeoTransformModel(
    val pageIndex: Int = 0,
    val pageWidthPt: Double,
    val pageHeightPt: Double,
    val geoStandard: GeoStandard,
    val epsgCode: Int?,
    val wktProjection: String?,
    val transformMatrix: TransformMatrix,
    val boundsLatLon: List<LatLon>,
    val bboxPagePoints: List<PagePoint>
) {
    /**
     * Converts WGS84 coordinate to PDF page point.
     * Returns null if coordinates fall outside the map boundary (with small tolerance) or conversion fails.
     */
    fun latLonToPagePoint(lat: Double, lon: Double): PagePoint? {
        if (!isInsideBounds(lat, lon)) {
            return null
        }
        return transformMatrix.toPagePoint(lat, lon)
    }

    /**
     * Converts PDF page point to WGS84 coordinate.
     */
    fun pagePointToLatLon(xPt: Double, yPt: Double): LatLon? {
        return transformMatrix.toLatLon(xPt, yPt)
    }

    /**
     * Determines whether a given coordinate falls within the map neatline polygon.
     * Ray-casting algorithm with vertex tolerance and bounding-box pre-filtering.
     */
    fun isInsideBounds(lat: Double, lon: Double): Boolean {
        if (boundsLatLon.size < 3) return true

        // Check if point matches any vertex within ~1 meter (1e-5 deg)
        for (pt in boundsLatLon) {
            if (kotlin.math.abs(lat - pt.latitude) < 1e-5 &&
                kotlin.math.abs(lon - pt.longitude) < 1e-5
            ) {
                return true
            }
        }

        // Fast bounding box check with slight buffer (~0.0001 deg / 11 meters)
        val minLat = boundsLatLon.minOf { it.latitude } - 0.0001
        val maxLat = boundsLatLon.maxOf { it.latitude } + 0.0001
        val minLon = boundsLatLon.minOf { it.longitude } - 0.0001
        val maxLon = boundsLatLon.maxOf { it.longitude } + 0.0001

        if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) {
            return false
        }

        // Perturb latitude slightly to avoid ray passing through exact vertices
        val testLat = lat + 1e-9

        // Ray casting algorithm
        var inside = false
        var j = boundsLatLon.size - 1
        for (i in boundsLatLon.indices) {
            val xi = boundsLatLon[i].longitude
            val yi = boundsLatLon[i].latitude
            val xj = boundsLatLon[j].longitude
            val yj = boundsLatLon[j].latitude

            val intersect = ((yi > testLat) != (yj > testLat)) &&
                    (lon < (xj - xi) * (testLat - yi) / (yj - yi) + xi)
            if (intersect) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
