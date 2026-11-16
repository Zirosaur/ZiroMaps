package com.zirosaur.ziromaps.core.geopdf

import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSFloat
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.zirosaur.ziromaps.core.geopdf.model.GeoStandard
import com.zirosaur.ziromaps.core.geopdf.parser.CompositeGeoPdfParser
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParseResult
import com.zirosaur.ziromaps.core.geopdf.parser.Iso32000Parser
import com.zirosaur.ziromaps.core.geopdf.parser.OgcLgiDictParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompositeGeoPdfParserTest {

    private lateinit var compositeParser: CompositeGeoPdfParser

    @Before
    fun setup() {
        compositeParser = CompositeGeoPdfParser(
            iso32000Parser = Iso32000Parser(),
            ogcLgiDictParser = OgcLgiDictParser()
        )
    }

    private fun getSampleIsoPdfFile(): File {
        val candidates = listOf(
            File("sample-data/BKPH RAWA TMR.pdf"),
            File("../../sample-data/BKPH RAWA TMR.pdf"),
            File("../sample-data/BKPH RAWA TMR.pdf"),
            File(System.getProperty("user.dir") ?: ".", "sample-data/BKPH RAWA TMR.pdf")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not find sample-data/BKPH RAWA TMR.pdf")
    }

    private fun createOgcPdfFile(): File {
        val file = File.createTempFile("ogc_composite_", ".pdf")
        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            val pageDict = page.cosObject

            val lgiDict = COSDictionary()
            lgiDict.setName(COSName.TYPE, "LGIDict")

            val ctmArray = COSArray()
            val ctm = listOf(0.0001, 0.0, 0.0, 0.0001, 108.0, -7.0)
            for (v in ctm) {
                ctmArray.add(COSFloat(v.toString()))
            }
            lgiDict.setItem(COSName.getPDFName("CTM"), ctmArray)

            pageDict.setItem(COSName.getPDFName("LGIDict"), lgiDict)
            doc.save(file)
        }
        return file
    }

    @Test
    fun parse_iso32000Sample_usesPriority1IsoParser() {
        val isoFile = getSampleIsoPdfFile()
        val result = compositeParser.parse(isoFile)

        assertTrue("Expected Success, got $result", result is GeoPdfParseResult.Success)
        val model = (result as GeoPdfParseResult.Success).model
        assertEquals(GeoStandard.ISO_32000, model.geoStandard)
    }

    @Test
    fun parse_ogcLgiDictPdf_fallsBackToPriority2OgcParser() {
        val ogcFile = createOgcPdfFile()
        try {
            val result = compositeParser.parse(ogcFile)
            assertTrue("Expected Success, got $result", result is GeoPdfParseResult.Success)
            val model = (result as GeoPdfParseResult.Success).model
            assertEquals(GeoStandard.OGC_LGIDICT, model.geoStandard)
        } finally {
            ogcFile.delete()
        }
    }

    @Test
    fun parse_plainPdf_returnsNotGeoPdf() {
        val tempPdf = File.createTempFile("plain_comp_test", ".pdf")
        try {
            PDDocument().use { doc ->
                doc.addPage(PDPage())
                doc.save(tempPdf)
            }
            val result = compositeParser.parse(tempPdf)
            assertEquals(GeoPdfParseResult.NotGeoPdf, result)
        } finally {
            tempPdf.delete()
        }
    }
}
