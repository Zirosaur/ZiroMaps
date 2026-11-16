package com.zirosaur.ziromaps.core.geopdf.parser

import com.zirosaur.ziromaps.core.geopdf.model.GeoTransformModel

/**
 * Result of parsing a GeoPDF document.
 */
sealed interface GeoPdfParseResult {
    /**
     * Successfully parsed a georeferenced PDF page.
     */
    data class Success(val model: GeoTransformModel) : GeoPdfParseResult

    /**
     * The PDF is valid but does not contain georeferencing metadata for the requested standard.
     */
    data object NotGeoPdf : GeoPdfParseResult

    /**
     * An error occurred while reading or parsing the PDF.
     */
    data class Error(val message: String, val cause: Throwable? = null) : GeoPdfParseResult
}
