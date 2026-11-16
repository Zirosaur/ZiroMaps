package com.zirosaur.ziromaps.core.geopdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.zirosaur.ziromaps.core.geopdf.model.GeoStandard
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParseResult
import com.zirosaur.ziromaps.core.geopdf.parser.Iso32000Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Iso32000ParserTest {

    private lateinit var parser: Iso32000Parser

    @Before
    fun setup() {
        parser = Iso32000Parser()
    }

    private fun getSamplePdfFile(): File {
        val candidates = listOf(
            File("sample-data/BKPH RAWA TMR.pdf"),
            File("../../sample-data/BKPH RAWA TMR.pdf"),
            File("../sample-data/BKPH RAWA TMR.pdf"),
            File(System.getProperty("user.dir") ?: ".", "sample-data/BKPH RAWA TMR.pdf")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not find sample-data/BKPH RAWA TMR.pdf in any known location")
    }

    @Test
    fun parse_sampleGeoPdf_successfullyExtractsMetadataAndGeometry() {
        val sampleFile = getSamplePdfFile()
        val result = parser.parse(sampleFile, pageIndex = 0)

        assertTrue("Expected GeoPdfParseResult.Success, got $result", result is GeoPdfParseResult.Success)
        val model = (result as GeoPdfParseResult.Success).model

        // Assert standard & projection
        assertEquals(GeoStandard.ISO_32000, model.geoStandard)
        assertEquals(32749, model.epsgCode)
        assertNotNull(model.wktProjection)
        assertTrue(model.wktProjection!!.contains("WGS_1984_UTM_Zone_49S"))

        // Assert page dimensions in points (approx 9921 x 8504 pt)
        assertEquals(9921.0, model.pageWidthPt, 1.0)
        assertEquals(8504.2, model.pageHeightPt, 1.0)

        // Assert bounds control points count (4 corners)
        assertEquals(4, model.boundsLatLon.size)
        assertEquals(4, model.bboxPagePoints.size)

        // Control point 0: Bottom-Left (South-West)
        val cp0 = model.boundsLatLon[0]
        assertEquals(-7.78735, cp0.latitude, 0.0001)
        assertEquals(108.80027, cp0.longitude, 0.0001)

        // Control point 1: Top-Left (North-West)
        val cp1 = model.boundsLatLon[1]
        assertEquals(-7.53352, cp1.latitude, 0.0001)
        assertEquals(108.80158, cp1.longitude, 0.0001)

        // Control point 2: Top-Right (North-East)
        val cp2 = model.boundsLatLon[2]
        assertEquals(-7.53495, cp2.latitude, 0.0001)
        assertEquals(109.10628, cp2.longitude, 0.0001)

        // Control point 3: Bottom-Right (South-East)
        val cp3 = model.boundsLatLon[3]
        assertEquals(-7.78883, cp3.latitude, 0.0001)
        assertEquals(109.10516, cp3.longitude, 0.0001)
    }

    @Test
    fun transformation_controlPoints_subMeterAccuracy() {
        val sampleFile = getSamplePdfFile()
        val result = parser.parse(sampleFile) as GeoPdfParseResult.Success
        val model = result.model

        // Verify forward and inverse accuracy on all 4 corners
        for (i in model.boundsLatLon.indices) {
            val geoExpected = model.boundsLatLon[i]
            val pageExpected = model.bboxPagePoints[i]

            // Page -> Geo
            val geoActual = model.pagePointToLatLon(pageExpected.xPt, pageExpected.yPt)
            assertNotNull(geoActual)
            val dLat = abs(geoActual!!.latitude - geoExpected.latitude)
            val dLon = abs(geoActual.longitude - geoExpected.longitude)
            // Error less than 0.00001 deg (~1 meter)
            assertTrue("Latitude error at CP $i too high: $dLat", dLat < 0.00001)
            assertTrue("Longitude error at CP $i too high: $dLon", dLon < 0.00001)

            // Geo -> Page
            val pageActual = model.latLonToPagePoint(geoExpected.latitude, geoExpected.longitude)
            assertNotNull(pageActual)
            val dX = abs(pageActual!!.xPt - pageExpected.xPt)
            val dY = abs(pageActual.yPt - pageExpected.yPt)
            // Error less than 0.01 pt (< 0.1 meter)
            assertTrue("X point error at CP $i too high: $dX", dX < 0.01)
            assertTrue("Y point error at CP $i too high: $dY", dY < 0.01)
        }
    }

    @Test
    fun transformation_insidePoint_roundTripConsistency() {
        val sampleFile = getSamplePdfFile()
        val result = parser.parse(sampleFile) as GeoPdfParseResult.Success
        val model = result.model

        // Map center: approx Lat -7.66, Lon 108.95
        val testLat = -7.66
        val testLon = 108.95

        assertTrue("Expected center to be inside bounds", model.isInsideBounds(testLat, testLon))

        val pagePt = model.latLonToPagePoint(testLat, testLon)
        assertNotNull("PagePoint should not be null for coordinate inside bounds", pagePt)

        // Page coordinates must be within the page boundaries
        assertTrue(pagePt!!.xPt > 0 && pagePt.xPt < model.pageWidthPt)
        assertTrue(pagePt.yPt > 0 && pagePt.yPt < model.pageHeightPt)

        // Round-trip back to LatLon
        val roundTripGeo = model.pagePointToLatLon(pagePt.xPt, pagePt.yPt)
        assertNotNull(roundTripGeo)
        assertEquals(testLat, roundTripGeo!!.latitude, 0.00005)
        assertEquals(testLon, roundTripGeo.longitude, 0.00005)
    }

    @Test
    fun transformation_outsidePoint_returnsNull() {
        val sampleFile = getSamplePdfFile()
        val result = parser.parse(sampleFile) as GeoPdfParseResult.Success
        val model = result.model

        // Jakarta coordinates (outside Central Java map boundary)
        val jakartaLat = -6.2088
        val jakartaLon = 106.8456

        val pagePt = model.latLonToPagePoint(jakartaLat, jakartaLon)
        assertNull("Points outside map neatline must return null", pagePt)
    }

    @Test
    fun transformMatrix_jsonSerializationRoundTrip() {
        val sampleFile = getSamplePdfFile()
        val result = parser.parse(sampleFile) as GeoPdfParseResult.Success
        val originalMatrix = result.model.transformMatrix

        val jsonString = originalMatrix.toJson()
        assertTrue(jsonString.isNotBlank())

        val restoredMatrix = TransformMatrix.fromJson(jsonString)
        assertEquals(originalMatrix.c0, restoredMatrix.c0, 1e-9)
        assertEquals(originalMatrix.c1, restoredMatrix.c1, 1e-9)
        assertEquals(originalMatrix.c2, restoredMatrix.c2, 1e-9)
        assertEquals(originalMatrix.c3, restoredMatrix.c3, 1e-9)
        assertEquals(originalMatrix.d0, restoredMatrix.d0, 1e-9)
        assertEquals(originalMatrix.d1, restoredMatrix.d1, 1e-9)
        assertEquals(originalMatrix.d2, restoredMatrix.d2, 1e-9)
        assertEquals(originalMatrix.d3, restoredMatrix.d3, 1e-9)
        assertEquals(originalMatrix.e0, restoredMatrix.e0, 1e-9)
        assertEquals(originalMatrix.e1, restoredMatrix.e1, 1e-9)
        assertEquals(originalMatrix.f0, restoredMatrix.f0, 1e-9)
        assertEquals(originalMatrix.f1, restoredMatrix.f1, 1e-9)

        assertEquals(originalMatrix.boundsLatLon.size, restoredMatrix.boundsLatLon.size)
        assertEquals(originalMatrix.bboxPagePoints.size, restoredMatrix.bboxPagePoints.size)
    }

    @Test
    fun parse_plainPdfWithoutGeoMetadata_returnsNotGeoPdf() {
        // Create an in-memory plain PDF without any georeferencing
        val tempPdf = File.createTempFile("plain_test", ".pdf")
        try {
            PDDocument().use { doc ->
                doc.addPage(PDPage())
                doc.save(tempPdf)
            }
            val result = parser.parse(tempPdf)
            assertEquals(GeoPdfParseResult.NotGeoPdf, result)
        } finally {
            tempPdf.delete()
        }
    }

    @Test
    fun parse_nonExistentFile_returnsError() {
        val nonExistent = File("/non_existent_path_to_map.pdf")
        val result = parser.parse(nonExistent)
        assertTrue("Expected GeoPdfParseResult.Error", result is GeoPdfParseResult.Error)
    }

    @Test
    fun extractEpsgCode_correctlyIdentifiesProjections() {
        // Test UTM South
        val utmSouthWkt = """PROJCS["WGS_1984_UTM_Zone_49S",GEOGCS["GCS_WGS_1984"]]"""
        assertEquals(32749, parser.extractEpsgCode(utmSouthWkt))

        // Test UTM North
        val utmNorthWkt = """PROJCS["WGS_1984_UTM_Zone_48N",GEOGCS["GCS_WGS_1984"]]"""
        assertEquals(32648, parser.extractEpsgCode(utmNorthWkt))

        // Test Authority match
        val epsgWkt = """GEOGCS["WGS 84",DATUM["WGS_1984"],AUTHORITY["EPSG","4326"]]"""
        assertEquals(4326, parser.extractEpsgCode(epsgWkt))

        // Test ID match (WKT2)
        val wkt2 = """GEOGCRS["WGS 84",ID["EPSG",4326]]"""
        assertEquals(4326, parser.extractEpsgCode(wkt2))
    }
}
