package com.zirosaur.ziromaps.core.geopdf

import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSFloat
import com.tom_roush.pdfbox.cos.COSInteger
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSString
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.zirosaur.ziromaps.core.geopdf.model.GeoStandard
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParseResult
import com.zirosaur.ziromaps.core.geopdf.parser.OgcLgiDictParser
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
class OgcLgiDictParserTest {

    private lateinit var parser: OgcLgiDictParser

    @Before
    fun setup() {
        parser = OgcLgiDictParser()
    }

    private fun createOgcPdfFile(
        ctm: List<Double>? = null,
        neatline: List<Double>? = null,
        registration: List<Double>? = null,
        projSetup: ((COSDictionary) -> Unit)? = null
    ): File {
        val file = File.createTempFile("ogc_test_", ".pdf")
        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            val pageDict = page.cosObject

            val lgiDict = COSDictionary()
            lgiDict.setName(COSName.TYPE, "LGIDict")
            lgiDict.setString(COSName.getPDFName("Version"), "2.1")

            if (ctm != null) {
                val ctmArray = COSArray()
                for (v in ctm) {
                    ctmArray.add(COSFloat(v.toString()))
                }
                lgiDict.setItem(COSName.getPDFName("CTM"), ctmArray)
            }

            if (neatline != null) {
                val neatlineArray = COSArray()
                for (v in neatline) {
                    neatlineArray.add(COSFloat(v.toString()))
                }
                lgiDict.setItem(COSName.getPDFName("Neatline"), neatlineArray)
            }

            if (registration != null) {
                val regArray = COSArray()
                for (v in registration) {
                    regArray.add(COSFloat(v.toString()))
                }
                lgiDict.setItem(COSName.getPDFName("Registration"), regArray)
            }

            if (projSetup != null) {
                val projDict = COSDictionary()
                projDict.setName(COSName.TYPE, "Projection")
                projSetup(projDict)
                lgiDict.setItem(COSName.getPDFName("Projection"), projDict)
            }

            pageDict.setItem(COSName.getPDFName("LGIDict"), lgiDict)
            doc.save(file)
        }
        return file
    }

    @Test
    fun parse_validOgcLgiDictWithCtm_successfullyParses() {
        // Sample affine CTM:
        // lon = a*x + c*y + e
        // lat = b*x + d*y + f
        // CTM = [a, b, c, d, e, f]
        // Let: a = 0.0001, b = 0.0, c = 0.0, d = 0.0001, e = 108.0, f = -7.0
        val ctm = listOf(0.0001, 0.0, 0.0, 0.0001, 108.0, -7.0)
        // Neatline polygon: BL (0,0), TL (0, 1000), TR (1000, 1000), BR (1000, 0)
        val neatline = listOf(0.0, 0.0, 0.0, 1000.0, 1000.0, 1000.0, 1000.0, 0.0)

        val testPdf = createOgcPdfFile(
            ctm = ctm,
            neatline = neatline,
            projSetup = { proj ->
                proj.setName(COSName.getPDFName("ProjectionType"), "GEOGRAPHIC")
                proj.setString(COSName.getPDFName("Datum"), "WGS 84")
                proj.setItem(COSName.getPDFName("EPSG"), COSInteger.get(4326))
            }
        )

        try {
            val result = parser.parse(testPdf)
            assertTrue("Expected Success, got $result", result is GeoPdfParseResult.Success)
            val model = (result as GeoPdfParseResult.Success).model

            assertEquals(GeoStandard.OGC_LGIDICT, model.geoStandard)
            assertEquals(4326, model.epsgCode)
            assertEquals(4, model.bboxPagePoints.size)
            assertEquals(4, model.boundsLatLon.size)

            // Test corner coordinates
            // BL: x=0, y=0 -> lon = 108.0, lat = -7.0
            val blGeo = model.pagePointToLatLon(0.0, 0.0)
            assertNotNull(blGeo)
            assertEquals(108.0, blGeo!!.longitude, 1e-6)
            assertEquals(-7.0, blGeo.latitude, 1e-6)

            // TR: x=1000, y=1000 -> lon = 108.1, lat = -6.9
            val trGeo = model.pagePointToLatLon(1000.0, 1000.0)
            assertNotNull(trGeo)
            assertEquals(108.1, trGeo!!.longitude, 1e-6)
            assertEquals(-6.9, trGeo.latitude, 1e-6)

            // Reverse: lat = -6.95, lon = 108.05 (center) -> x = 500, y = 500
            val centerPt = model.latLonToPagePoint(-6.95, 108.05)
            assertNotNull(centerPt)
            assertEquals(500.0, centerPt!!.xPt, 0.1)
            assertEquals(500.0, centerPt.yPt, 0.1)

            // Outside neatline: lat = -6.5, lon = 108.05 -> should return null
            val outsidePt = model.latLonToPagePoint(-6.5, 108.05)
            assertNull(outsidePt)
        } finally {
            testPdf.delete()
        }
    }

    @Test
    fun parse_ogcLgiDictWithRegistrationFallback_successfullyParses() {
        // 4 registration points: [xPt, yPt, lon, lat, ...]
        val reg = listOf(
            0.0, 0.0, 108.0, -7.0,
            0.0, 800.0, 108.0, -6.5,
            800.0, 800.0, 108.5, -6.5,
            800.0, 0.0, 108.5, -7.0
        )

        val testPdf = createOgcPdfFile(
            registration = reg,
            projSetup = { proj ->
                proj.setItem(COSName.getPDFName("Zone"), COSInteger.get(49))
                proj.setName(COSName.getPDFName("ProjectionType"), "UTM")
                proj.setString(COSName.getPDFName("Hemisphere"), "S")
            }
        )

        try {
            val result = parser.parse(testPdf)
            assertTrue("Expected Success, got $result", result is GeoPdfParseResult.Success)
            val model = (result as GeoPdfParseResult.Success).model

            assertEquals(GeoStandard.OGC_LGIDICT, model.geoStandard)
            assertEquals(32749, model.epsgCode)
            assertEquals(4, model.boundsLatLon.size)

            val p0 = model.pagePointToLatLon(0.0, 0.0)
            assertNotNull(p0)
            assertEquals(108.0, p0!!.longitude, 1e-5)
            assertEquals(-7.0, p0.latitude, 1e-5)
        } finally {
            testPdf.delete()
        }
    }

    @Test
    fun parse_ogcLgiDictWithDegenerateCtm_returnsError() {
        // Degenerate CTM (all zeros, det = 0)
        val degenerateCtm = listOf(0.0, 0.0, 0.0, 0.0, 100.0, 200.0)
        val testPdf = createOgcPdfFile(ctm = degenerateCtm)

        try {
            val result = parser.parse(testPdf)
            assertTrue("Expected Error for degenerate CTM", result is GeoPdfParseResult.Error)
        } finally {
            testPdf.delete()
        }
    }

    @Test
    fun parse_plainPdfWithoutLgiDict_returnsNotGeoPdf() {
        val tempPdf = File.createTempFile("plain_ogc_test", ".pdf")
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
    fun extractEpsg_variousProjectionFormats() {
        val projDict = COSDictionary()
        projDict.setItem(COSName.getPDFName("Zone"), COSInteger.get(49))
        projDict.setString(COSName.getPDFName("ProjectionType"), "UTM")
        projDict.setString(COSName.getPDFName("Hemisphere"), "N")

        val epsgNorth = parser.extractEpsg(projDict, null)
        assertEquals(32649, epsgNorth)

        projDict.setString(COSName.getPDFName("Hemisphere"), "S")
        val epsgSouth = parser.extractEpsg(projDict, null)
        assertEquals(32749, epsgSouth)

        projDict.setItem(COSName.getPDFName("EPSG"), COSInteger.get(3857))
        val epsgDirect = parser.extractEpsg(projDict, null)
        assertEquals(3857, epsgDirect)
    }
}
