package com.zirosaur.ziromaps.core.geopdf.parser

import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSNumber
import com.tom_roush.pdfbox.cos.COSString
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.zirosaur.ziromaps.core.geopdf.model.GeoStandard
import com.zirosaur.ziromaps.core.geopdf.model.GeoTransformModel
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * GeoPDF Parser implementing ISO 32000-1 ExtensionLevel 3 Geospatial Features.
 *
 * Extracts Viewport (/VP) dictionaries, Geospatial Measure (/Measure, Subtype /GEO),
 * polygon bounds (/Bounds), ground coordinates (/GPTS), and coordinate reference system (/GCS or /DCS).
 */
@Singleton
class Iso32000Parser @Inject constructor() : GeoPdfParser {

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

        // Extract Viewport (/VP) array or dictionary
        val vpObject = cosPage.getDictionaryObject(COSName.getPDFName("VP"))
            ?: return GeoPdfParseResult.NotGeoPdf

        val vpDict = when (vpObject) {
            is COSArray -> {
                if (vpObject.size() == 0) return GeoPdfParseResult.NotGeoPdf
                vpObject.get(0) as? COSDictionary
            }
            is COSDictionary -> vpObject
            else -> null
        } ?: return GeoPdfParseResult.NotGeoPdf

        // Extract Viewport BBox
        val bboxArray = vpDict.getDictionaryObject(COSName.getPDFName("BBox")) as? COSArray
            ?: return GeoPdfParseResult.NotGeoPdf
        if (bboxArray.size() < 4) {
            return GeoPdfParseResult.NotGeoPdf
        }

        val b0 = (bboxArray.get(0) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val b1 = (bboxArray.get(1) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val b2 = (bboxArray.get(2) as? COSNumber)?.toDoublePrecision() ?: 0.0
        val b3 = (bboxArray.get(3) as? COSNumber)?.toDoublePrecision() ?: 0.0

        val bMinX = min(b0, b2)
        val bMaxX = max(b0, b2)
        val bMinY = min(b1, b3)
        val bMaxY = max(b1, b3)

        // Extract Measure dictionary
        val measureDict = vpDict.getDictionaryObject(COSName.getPDFName("Measure")) as? COSDictionary
            ?: return GeoPdfParseResult.NotGeoPdf

        // Verify Subtype is GEO
        val subtype = measureDict.getCOSName(COSName.SUBTYPE)
            ?: (measureDict.getDictionaryObject(COSName.getPDFName("Subtype")) as? COSName)
        if (subtype?.name != "GEO") {
            return GeoPdfParseResult.NotGeoPdf
        }

        // Extract GPTS (Ground Points: Lat, Lon pairs)
        val gptsArray = measureDict.getDictionaryObject(COSName.getPDFName("GPTS")) as? COSArray
            ?: return GeoPdfParseResult.NotGeoPdf
        if (gptsArray.size() < 6 || gptsArray.size() % 2 != 0) {
            return GeoPdfParseResult.Error("Invalid GPTS array size (${gptsArray.size()}), expected at least 6 coordinates")
        }

        val pointCount = gptsArray.size() / 2
        val geoPoints = mutableListOf<LatLon>()
        for (i in 0 until pointCount) {
            val lat = (gptsArray.get(2 * i) as? COSNumber)?.toDoublePrecision() ?: 0.0
            val lon = (gptsArray.get(2 * i + 1) as? COSNumber)?.toDoublePrecision() ?: 0.0
            geoPoints.add(LatLon(latitude = lat, longitude = lon))
        }

        // Extract Bounds (normalized viewport coordinates [u, v])
        val boundsArray = measureDict.getDictionaryObject(COSName.getPDFName("Bounds")) as? COSArray
        val pagePoints = mutableListOf<PagePoint>()

        if (boundsArray != null && boundsArray.size() >= 2 * pointCount) {
            for (i in 0 until pointCount) {
                val u = (boundsArray.get(2 * i) as? COSNumber)?.toDoublePrecision() ?: 0.0
                val v = (boundsArray.get(2 * i + 1) as? COSNumber)?.toDoublePrecision() ?: 0.0
                val x = bMinX + u * (bMaxX - bMinX)
                val y = bMaxY - v * (bMaxY - bMinY)
                pagePoints.add(PagePoint(xPt = x, yPt = y))
            }
        } else {
            // Default 4-corner mapping
            val defaultNorm = listOf(
                Pair(0.0, 1.0), // bottom-left
                Pair(0.0, 0.0), // top-left
                Pair(1.0, 0.0), // top-right
                Pair(1.0, 1.0)  // bottom-right
            )
            for (i in 0 until min(pointCount, defaultNorm.size)) {
                val (u, v) = defaultNorm[i]
                val x = bMinX + u * (bMaxX - bMinX)
                val y = bMaxY - v * (bMaxY - bMinY)
                pagePoints.add(PagePoint(xPt = x, yPt = y))
            }
        }

        // Extract GCS or DCS dictionary
        val gcsDict = (measureDict.getDictionaryObject(COSName.getPDFName("GCS"))
            ?: measureDict.getDictionaryObject(COSName.getPDFName("DCS"))) as? COSDictionary

        val wkt = extractWktString(gcsDict)
        val epsgCode = wkt?.let { extractEpsgCode(it) }

        // Compute page dimensions in points
        val cropBox = page.cropBox ?: page.mediaBox
        val pageWidth = cropBox?.width?.toDouble() ?: (bMaxX - bMinX)
        val pageHeight = cropBox?.height?.toDouble() ?: (bMaxY - bMinY)

        val transformMatrix = try {
            TransformMatrix.fromControlPoints(
                pagePoints = pagePoints,
                geoPoints = geoPoints
            )
        } catch (e: Exception) {
            return GeoPdfParseResult.Error("Failed to calculate transform matrix: ${e.message}", e)
        }

        val model = GeoTransformModel(
            pageIndex = pageIndex,
            pageWidthPt = pageWidth,
            pageHeightPt = pageHeight,
            geoStandard = GeoStandard.ISO_32000,
            epsgCode = epsgCode,
            wktProjection = wkt,
            transformMatrix = transformMatrix,
            boundsLatLon = geoPoints,
            bboxPagePoints = pagePoints
        )

        return GeoPdfParseResult.Success(model)
    }

    private fun extractWktString(gcsDict: COSDictionary?): String? {
        if (gcsDict == null) return null
        val wktObj = gcsDict.getDictionaryObject(COSName.getPDFName("WKT")) ?: return null
        return when (wktObj) {
            is COSString -> wktObj.string
            is COSName -> wktObj.name
            else -> gcsDict.getString(COSName.getPDFName("WKT"))
        }
    }

    private val epsgAuthorityRegex = Regex("""AUTHORITY\["EPSG",\s*"(\d+)"\]""", RegexOption.IGNORE_CASE)
    private val epsgIdRegex = Regex("""ID\["EPSG",\s*(\d+)\]""", RegexOption.IGNORE_CASE)
    private val utmZoneRegex = Regex("""UTM[ _]Zone[ _](\d+)\s*([NS])""", RegexOption.IGNORE_CASE)

    internal fun extractEpsgCode(wkt: String): Int? {
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
        return null
    }

    @Suppress("DEPRECATION")
    private fun COSNumber.toDoublePrecision(): Double = doubleValue()
}
