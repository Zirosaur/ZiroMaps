package com.zirosaur.ziromaps.core.geopdf.parser

import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite GeoPDF parser implementing the specification fallback order:
 * 1. ISO 32000-1 ExtensionLevel 3 (Priority 1 — Modern QGIS / ArcMap standard)
 * 2. OGC GeoPDF 2.2 / TerraGo LGIDict (Priority 2 — Legacy ArcGIS / TerraGo standard)
 */
@Singleton
class CompositeGeoPdfParser @Inject constructor(
    private val iso32000Parser: Iso32000Parser,
    private val ogcLgiDictParser: OgcLgiDictParser
) : GeoPdfParser {

    override fun parse(file: File, pageIndex: Int): GeoPdfParseResult {
        // Priority 1: ISO 32000-1
        val isoResult = iso32000Parser.parse(file, pageIndex)
        if (isoResult !is GeoPdfParseResult.NotGeoPdf) {
            return isoResult
        }

        // Priority 2: OGC LGIDict fallback
        return ogcLgiDictParser.parse(file, pageIndex)
    }

    override fun parse(inputStream: InputStream, pageIndex: Int): GeoPdfParseResult {
        // Since InputStream cannot be rewound after read, copy stream bytes to memory or temp file
        val tempFile = try {
            File.createTempFile("geopdf_stream_", ".pdf").apply {
                deleteOnExit()
                outputStream().use { out ->
                    inputStream.copyTo(out)
                }
            }
        } catch (e: Exception) {
            return GeoPdfParseResult.Error("Failed to buffer input stream: ${e.message}", e)
        }

        return try {
            parse(tempFile, pageIndex)
        } finally {
            tempFile.delete()
        }
    }
}
