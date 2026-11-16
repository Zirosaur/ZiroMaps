package com.zirosaur.ziromaps.core.geopdf.parser

import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSNumber
import com.tom_roush.pdfbox.cos.COSString
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.zirosaur.ziromaps.core.geopdf.model.GeoStandard
import com.zirosaur.ziromaps.core.geopdf.model.GeoTransformModel
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * GeoPDF Parser implementing OGC GeoPDF / TerraGo LGIDict Best Practice (OGC 08-139).
 *
 * Extracts LGIDict dictionaries, Coordinate Transformation Matrix (/CTM),
 * Projection parameters, and neatline bounding polygons (/Neatline).
 */
@Singleton
class OgcLgiDictParser @Inject constructor() : GeoPdfParser {

    override fun parse(file: File, pageIndex: Int): GeoPdfParseResult {
        return try {
            PDDocument.load(file).use { document ->
                parseDocument(document, pageIndex)
            }
        } catch (e: Exception) {
            GeoPdfParseResult.Error("Failed to parse PDF file: ${e.message}", e)
        }
    }

    override fun parse(inputStream: InputStream, pageIndex: Int): GeoPdfParseResult {
        return try {
            PDDocument.load(inputStream).use { document ->
                parseDocument(document, pageIndex)
            }
        } catch (e: Exception) {
            GeoPdfParseResult.Error("Failed to parse PDF stream: ${e.message}", e)
        }
    }

    private fun parseDocument(document: PDDocument, pageIndex: Int): GeoPdfParseResult {
        if (pageIndex < 0 || pageIndex >= document.numberOfPages) {
            return GeoPdfParseResult.Error("Page index $pageIndex is out of range [0, ${document.numberOfPages - 1}]")
        }

        val page = document.getPage(pageIndex)
        val cosPage = page.cosObject

        // Extract LGIDict dictionary or array
        val lgiObj = cosPage.getDictionaryObject(COSName.getPDFName("LGIDict"))
            ?: return GeoPdfParseResult.NotGeoPdf

        val lgiDict = when (lgiObj) {
            is COSArray -> {
                if (lgiObj.size() == 0) return GeoPdfParseResult.NotGeoPdf
                lgiObj.get(0) as? COSDictionary
            }
            is COSDictionary -> lgiObj
            else -> null
        } ?: return GeoPdfParseResult.NotGeoPdf

        // Page dimensions in points
        val cropBox = page.cropBox ?: page.mediaBox
        val pageWidth = cropBox?.width?.toDouble() ?: 0.0
        val pageHeight = cropBox?.height?.toDouble() ?: 0.0

        // Extract Projection dictionary
        val projDict = lgiDict.getDictionaryObject(COSName.getPDFName("Projection")) as? COSDictionary
        val wkt = extractWkt(projDict)
        val epsgCode = extractEpsg(projDict, wkt)

        // Try extracting CTM (Coordinate Transformation Matrix)
        val ctmArray = lgiDict.getDictionaryObject(COSName.getPDFName("CTM")) as? COSArray
        if (ctmArray != null && ctmArray.size() >= 6) {
            return parseWithCtm(
                lgiDict = lgiDict,
                ctmArray = ctmArray,
                pageIndex = pageIndex,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                wkt = wkt,
                epsgCode = epsgCode
            )
        }

        // Fallback: Try Registration control points array
        val regArray = lgiDict.getDictionaryObject(COSName.getPDFName("Registration")) as? COSArray
        if (regArray != null && regArray.size() >= 12 && regArray.size() % 4 == 0) {
            return parseWithRegistration(
                lgiDict = lgiDict,
                regArray = regArray,
                pageIndex = pageIndex,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                wkt = wkt,
                epsgCode = epsgCode
            )
        }

        return GeoPdfParseResult.NotGeoPdf
    }

    private fun parseWithCtm(
        lgiDict: COSDictionary,
        ctmArray: COSArray,
        pageIndex: Int,
        pageWidth: Double,
        pageHeight: Double,
        wkt: String?,
        epsgCode: Int?
    ): GeoPdfParseResult {
        val a = (ctmArray.get(0) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val b = (ctmArray.get(1) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val c = (ctmArray.get(2) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val d = (ctmArray.get(3) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val e = (ctmArray.get(4) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val f = (ctmArray.get(5) as? COSNumber)?.toDoublePrecision() ?: 0.0

        val det = a * d - b * c
        if (abs(det) < 1e-15) {
            return GeoPdfParseResult.Error("Degenerate CTM with near-zero determinant: $det")
        }

        // Extract Neatline (PDF user points polygon)
        val neatlineArray = lgiDict.getDictionaryObject(COSName.getPDFName("Neatline")) as? COSArray
        val pagePoints = mutableListOf<PagePoint>()
        val geoPoints = mutableListOf<LatLon>()

        if (neatlineArray != null && neatlineArray.size() >= 6 && neatlineArray.size() % 2 == 0) {
            val pointCount = neatlineArray.size() / 2
            for (i in 0 until pointCount) {
                val x = (neatlineArray.get(2 * i) as? COSNumber)?.toDoublePrecision() ?: 0.0
                val y = (neatlineArray.get(2 * i + 1) as? COSNumber)?.toDoublePrecision() ?: 0.0
                pagePoints.add(PagePoint(x, y))

                val lon = a * x + c * y + e
                val lat = b * x + d * y + f
                geoPoints.add(LatLon(latitude = lat, longitude = lon))
            }
        } else {
            // Default neatline to page corners: BL, TL, TR, BR
            val corners = listOf(
                PagePoint(0.0, 0.0),
                PagePoint(0.0, pageHeight),
                PagePoint(pageWidth, pageHeight),
                PagePoint(pageWidth, 0.0)
            )
            for (pt in corners) {
                pagePoints.add(pt)
                val lon = a * pt.xPt + c * pt.yPt + e
                val lat = b * pt.xPt + d * pt.yPt + f
                geoPoints.add(LatLon(latitude = lat, longitude = lon))
            }
        }

        val matrix = TransformMatrix(
            // Forward: xPt, yPt -> lon, lat
            // lon = c0 + c1*x + c2*y
            // lat = d0 + d1*x + d2*y
            c0 = e, c1 = a, c2 = c, c3 = 0.0,
            d0 = f, d1 = b, d2 = d, d3 = 0.0,
            // Inverse: lon, lat -> xPt, yPt
            // x = e0 + e1*lon + e2*lat
            // y = f0 + f1*lon + f2*lat
            e0 = (-d * e + c * f) / det,
            e1 = d / det,
            e2 = -c / det,
            e3 = 0.0,
            f0 = (b * e - a * f) / det,
            f1 = -b / det,
            f2 = a / det,
            f3 = 0.0,
            boundsLatLon = geoPoints,
            bboxPagePoints = pagePoints
        )

        val model = GeoTransformModel(
            pageIndex = pageIndex,
            pageWidthPt = pageWidth,
            pageHeightPt = pageHeight,
            geoStandard = GeoStandard.OGC_LGIDICT,
            epsgCode = epsgCode,
            wktProjection = wkt,
            transformMatrix = matrix,
            boundsLatLon = geoPoints,
            bboxPagePoints = pagePoints
        )

        return GeoPdfParseResult.Success(model)
    }

    private fun parseWithRegistration(
        lgiDict: COSDictionary,
        regArray: COSArray,
        pageIndex: Int,
        pageWidth: Double,
        pageHeight: Double,
        wkt: String?,
        epsgCode: Int?
    ): GeoPdfParseResult {
        // Registration format: [xPt_0, yPt_0, lon_0, lat_0, xPt_1, yPt_1, lon_1, lat_1, ...]
        val pointCount = regArray.size() / 4
        val pagePoints = mutableListOf<PagePoint>()
        val geoPoints = mutableListOf<LatLon>()

        for (i in 0 until pointCount) {
            val x = (regArray.get(4 * i) as? COSNumber)?.toDoublePrecision() ?: 0.0
            val y = (regArray.get(4 * i + 1) as? COSNumber)?.toDoublePrecision() ?: 0.0
            val lon = (regArray.get(4 * i + 2) as? COSNumber)?.toDoublePrecision() ?: 0.0
            val lat = (regArray.get(4 * i + 3) as? COSNumber)?.toDoublePrecision() ?: 0.0
            pagePoints.add(PagePoint(x, y))
            geoPoints.add(LatLon(latitude = lat, longitude = lon))
        }

        val matrix = try {
            TransformMatrix.fromControlPoints(pagePoints, geoPoints)
        } catch (e: Exception) {
            return GeoPdfParseResult.Error("Failed to calculate transform matrix from Registration: ${e.message}", e)
        }

        val model = GeoTransformModel(
            pageIndex = pageIndex,
            pageWidthPt = pageWidth,
            pageHeightPt = pageHeight,
            geoStandard = GeoStandard.OGC_LGIDICT,
            epsgCode = epsgCode,
            wktProjection = wkt,
            transformMatrix = matrix,
            boundsLatLon = geoPoints,
            bboxPagePoints = pagePoints
        )

        return GeoPdfParseResult.Success(model)
    }

    private fun extractWkt(projDict: COSDictionary?): String? {
        if (projDict == null) return null
        val wktObj = projDict.getDictionaryObject(COSName.getPDFName("WKT")) ?: return null
        return when (wktObj) {
            is COSString -> wktObj.string
            is COSName -> wktObj.name
            else -> projDict.getString(COSName.getPDFName("WKT"))
        }
    }

    private val epsgAuthorityRegex = Regex("""AUTHORITY\["EPSG",\s*"(\d+)"\]""", RegexOption.IGNORE_CASE)
    private val epsgIdRegex = Regex("""ID\["EPSG",\s*(\d+)\]""", RegexOption.IGNORE_CASE)
    private val utmZoneRegex = Regex("""UTM[ _]Zone[ _](\d+)\s*([NS])""", RegexOption.IGNORE_CASE)

    internal fun extractEpsg(projDict: COSDictionary?, wkt: String?): Int? {
        // 1. Direct /EPSG key in Projection dictionary
        if (projDict != null) {
            val epsgObj = projDict.getDictionaryObject(COSName.getPDFName("EPSG"))
            when (epsgObj) {
                is COSNumber -> return epsgObj.intValue()
                is COSString -> epsgObj.string.trim().toIntOrNull()?.let { return it }
                else -> {}
            }

            // 2. ProjectionType and Zone
            val projType = (projDict.getDictionaryObject(COSName.getPDFName("ProjectionType")) as? COSName)?.name
                ?: projDict.getString(COSName.getPDFName("ProjectionType"))
            val zoneObj = projDict.getDictionaryObject(COSName.getPDFName("Zone"))
            val zone = when (zoneObj) {
                is COSNumber -> zoneObj.intValue()
                is COSString -> zoneObj.string.trim().toIntOrNull()
                else -> null
            }
            val hemiObj = projDict.getDictionaryObject(COSName.getPDFName("Hemisphere"))
            val hemisphere = when (hemiObj) {
                is COSString -> hemiObj.string.trim().uppercase()
                is COSName -> hemiObj.name.trim().uppercase()
                else -> "N"
            }

            if (zone != null && (projType == "UTM" || projType == "TC" || projType == "TransverseMercator")) {
                return if (hemisphere == "S") 32700 + zone else 32600 + zone
            }

            if (projType == "GEOGRAPHIC") {
                return 4326
            }
        }

        // 3. From WKT string
        if (wkt != null) {
            epsgAuthorityRegex.find(wkt)?.let { match ->
                return match.groupValues[1].toIntOrNull()
            }
            epsgIdRegex.find(wkt)?.let { match ->
                return match.groupValues[1].toIntOrNull()
            }
            utmZoneRegex.find(wkt)?.let { match ->
                val zone = match.groupValues[1].toIntOrNull() ?: return null
                val hemisphere = match.groupValues[2].uppercase()
                return if (hemisphere == "S") 32700 + zone else 32600 + zone
            }
            if (wkt.contains("WGS_1984", ignoreCase = true) || wkt.contains("WGS 84", ignoreCase = true)) {
                return 4326
            }
        }

        return null
    }

    @Suppress("DEPRECATION")
    private fun COSNumber.toDoublePrecision(): Double = doubleValue()
}
