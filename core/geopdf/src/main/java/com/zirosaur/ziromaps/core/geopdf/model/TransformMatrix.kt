package com.zirosaur.ziromaps.core.geopdf.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/**
 * Deterministic 2D coordinate transformation matrix between PDF page points and WGS84 coordinates.
 *
 * Forward mapping (PagePoint -> LatLon):
 * lon = c0 + c1*x + c2*y + c3*x*y
 * lat = d0 + d1*x + d2*y + d3*x*y
 *
 * Inverse mapping (LatLon -> PagePoint):
 * x = e0 + e1*lon + e2*lat + e3*lon*lat
 * y = f0 + f1*lon + f2*lat + f3*lon*lat
 */
data class TransformMatrix(
    val c0: Double,
    val c1: Double,
    val c2: Double,
    val c3: Double = 0.0,
    val d0: Double,
    val d1: Double,
    val d2: Double,
    val d3: Double = 0.0,
    val e0: Double,
    val e1: Double,
    val e2: Double,
    val e3: Double = 0.0,
    val f0: Double,
    val f1: Double,
    val f2: Double,
    val f3: Double = 0.0,
    val boundsLatLon: List<LatLon> = emptyList(),
    val bboxPagePoints: List<PagePoint> = emptyList()
) {
    fun toLatLon(xPt: Double, yPt: Double): LatLon {
        val lon = c0 + c1 * xPt + c2 * yPt + c3 * xPt * yPt
        val lat = d0 + d1 * xPt + d2 * yPt + d3 * xPt * yPt
        return LatLon(latitude = lat, longitude = lon)
    }

    fun toPagePoint(lat: Double, lon: Double): PagePoint {
        val x = e0 + e1 * lon + e2 * lat + e3 * lon * lat
        val y = f0 + f1 * lon + f2 * lat + f3 * lon * lat
        return PagePoint(xPt = x, yPt = y)
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("c0", c0)
        json.put("c1", c1)
        json.put("c2", c2)
        json.put("c3", c3)
        json.put("d0", d0)
        json.put("d1", d1)
        json.put("d2", d2)
        json.put("d3", d3)
        json.put("e0", e0)
        json.put("e1", e1)
        json.put("e2", e2)
        json.put("e3", e3)
        json.put("f0", f0)
        json.put("f1", f1)
        json.put("f2", f2)
        json.put("f3", f3)

        val boundsArray = JSONArray()
        for (pt in boundsLatLon) {
            val ptObj = JSONObject()
            ptObj.put("lat", pt.latitude)
            ptObj.put("lon", pt.longitude)
            boundsArray.put(ptObj)
        }
        json.put("boundsLatLon", boundsArray)

        val bboxArray = JSONArray()
        for (pt in bboxPagePoints) {
            val ptObj = JSONObject()
            ptObj.put("x", pt.xPt)
            ptObj.put("y", pt.yPt)
            bboxArray.put(ptObj)
        }
        json.put("bboxPagePoints", bboxArray)

        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): TransformMatrix {
            val json = JSONObject(jsonStr)
            val boundsList = mutableListOf<LatLon>()
            if (json.has("boundsLatLon")) {
                val arr = json.getJSONArray("boundsLatLon")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    boundsList.add(LatLon(obj.getDouble("lat"), obj.getDouble("lon")))
                }
            }

            val bboxList = mutableListOf<PagePoint>()
            if (json.has("bboxPagePoints")) {
                val arr = json.getJSONArray("bboxPagePoints")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    bboxList.add(PagePoint(obj.getDouble("x"), obj.getDouble("y")))
                }
            }

            return TransformMatrix(
                c0 = json.optDouble("c0", 0.0),
                c1 = json.optDouble("c1", 0.0),
                c2 = json.optDouble("c2", 0.0),
                c3 = json.optDouble("c3", 0.0),
                d0 = json.optDouble("d0", 0.0),
                d1 = json.optDouble("d1", 0.0),
                d2 = json.optDouble("d2", 0.0),
                d3 = json.optDouble("d3", 0.0),
                e0 = json.optDouble("e0", 0.0),
                e1 = json.optDouble("e1", 0.0),
                e2 = json.optDouble("e2", 0.0),
                e3 = json.optDouble("e3", 0.0),
                f0 = json.optDouble("f0", 0.0),
                f1 = json.optDouble("f1", 0.0),
                f2 = json.optDouble("f2", 0.0),
                f3 = json.optDouble("f3", 0.0),
                boundsLatLon = boundsList,
                bboxPagePoints = bboxList
            )
        }

        /**
         * Calculates forward and inverse transformation coefficients from matching control points.
         * Uses bilinear quadrilateral mapping via least-squares normal equations when >= 4 points are given,
         * or 6-parameter affine transformation when 3 points are given.
         */
        fun fromControlPoints(
            pagePoints: List<PagePoint>,
            geoPoints: List<LatLon>
        ): TransformMatrix {
            require(pagePoints.size >= 3 && pagePoints.size == geoPoints.size) {
                "At least 3 matching control points are required (found ${pagePoints.size} page points and ${geoPoints.size} geo points)"
            }

            val m = pagePoints.size
            val isBilinear = m >= 4

            if (isBilinear) {
                // Forward: Page -> Geo
                // lon = c0 + c1*x + c2*y + c3*x*y
                // lat = d0 + d1*x + d2*y + d3*x*y
                val aFwd = Array(m) { i ->
                    val p = pagePoints[i]
                    doubleArrayOf(1.0, p.xPt, p.yPt, p.xPt * p.yPt)
                }
                val bLon = DoubleArray(m) { i -> geoPoints[i].longitude }
                val bLat = DoubleArray(m) { i -> geoPoints[i].latitude }

                val cCoef = solveNormalEquations(aFwd, bLon)
                val dCoef = solveNormalEquations(aFwd, bLat)

                // Inverse: Geo -> Page
                // x = e0 + e1*lon + e2*lat + e3*lon*lat
                // y = f0 + f1*lon + f2*lat + f3*lon*lat
                val aInv = Array(m) { i ->
                    val g = geoPoints[i]
                    doubleArrayOf(1.0, g.longitude, g.latitude, g.longitude * g.latitude)
                }
                val bX = DoubleArray(m) { i -> pagePoints[i].xPt }
                val bY = DoubleArray(m) { i -> pagePoints[i].yPt }

                val eCoef = solveNormalEquations(aInv, bX)
                val fCoef = solveNormalEquations(aInv, bY)

                return TransformMatrix(
                    c0 = cCoef[0], c1 = cCoef[1], c2 = cCoef[2], c3 = cCoef[3],
                    d0 = dCoef[0], d1 = dCoef[1], d2 = dCoef[2], d3 = dCoef[3],
                    e0 = eCoef[0], e1 = eCoef[1], e2 = eCoef[2], e3 = eCoef[3],
                    f0 = fCoef[0], f1 = fCoef[1], f2 = fCoef[2], f3 = fCoef[3],
                    boundsLatLon = geoPoints,
                    bboxPagePoints = pagePoints
                )
            } else {
                // 3 points: Affine mapping
                val aFwd = Array(m) { i ->
                    val p = pagePoints[i]
                    doubleArrayOf(1.0, p.xPt, p.yPt)
                }
                val bLon = DoubleArray(m) { i -> geoPoints[i].longitude }
                val bLat = DoubleArray(m) { i -> geoPoints[i].latitude }

                val cCoef = solveNormalEquations(aFwd, bLon)
                val dCoef = solveNormalEquations(aFwd, bLat)

                val aInv = Array(m) { i ->
                    val g = geoPoints[i]
                    doubleArrayOf(1.0, g.longitude, g.latitude)
                }
                val bX = DoubleArray(m) { i -> pagePoints[i].xPt }
                val bY = DoubleArray(m) { i -> pagePoints[i].yPt }

                val eCoef = solveNormalEquations(aInv, bX)
                val fCoef = solveNormalEquations(aInv, bY)

                return TransformMatrix(
                    c0 = cCoef[0], c1 = cCoef[1], c2 = cCoef[2], c3 = 0.0,
                    d0 = dCoef[0], d1 = dCoef[1], d2 = dCoef[2], d3 = 0.0,
                    e0 = eCoef[0], e1 = eCoef[1], e2 = eCoef[2], e3 = 0.0,
                    f0 = fCoef[0], f1 = fCoef[1], f2 = fCoef[2], f3 = 0.0,
                    boundsLatLon = geoPoints,
                    bboxPagePoints = pagePoints
                )
            }
        }

        private fun solveNormalEquations(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val m = a.size
            val n = a[0].size
            val atA = Array(n) { i ->
                DoubleArray(n) { j ->
                    var sum = 0.0
                    for (k in 0 until m) {
                        sum += a[k][i] * a[k][j]
                    }
                    sum
                }
            }
            val atB = DoubleArray(n) { i ->
                var sum = 0.0
                for (k in 0 until m) {
                    sum += a[k][i] * b[k]
                }
                sum
            }
            return solveLinearSystem(atA, atB)
        }

        private fun solveLinearSystem(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val n = b.size
            val matrix = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }

            for (i in 0 until n) {
                var maxRow = i
                var maxVal = abs(matrix[i][i])
                for (k in i + 1 until n) {
                    val v = abs(matrix[k][i])
                    if (v > maxVal) {
                        maxVal = v
                        maxRow = k
                    }
                }
                if (maxVal < 1e-12) {
                    throw IllegalArgumentException("Singular matrix or degenerate control points")
                }
                val temp = matrix[i]
                matrix[i] = matrix[maxRow]
                matrix[maxRow] = temp

                for (k in i + 1 until n) {
                    val factor = matrix[k][i] / matrix[i][i]
                    for (j in i until n + 1) {
                        matrix[k][j] -= factor * matrix[i][j]
                    }
                }
            }

            val x = DoubleArray(n)
            for (i in n - 1 downTo 0) {
                var sum = matrix[i][n]
                for (j in i + 1 until n) {
                    sum -= matrix[i][j] * x[j]
                }
                x[i] = sum / matrix[i][i]
            }
            return x
        }
    }
}
