package com.zirosaur.ziromaps.core.geopdf.parser

import java.io.File
import java.io.InputStream

/**
 * Contract for parsing georeferencing metadata from PDF files.
 */
interface GeoPdfParser {
    /**
     * Parses georeferencing metadata from a local [File].
     *
     * @param file the PDF file on disk
     * @param pageIndex 0-based page index to parse (default 0)
     * @return [GeoPdfParseResult] representing success, non-georeferenced, or error.
     */
    fun parse(file: File, pageIndex: Int = 0): GeoPdfParseResult

    /**
     * Parses georeferencing metadata from an [InputStream].
     *
     * @param inputStream the input stream containing PDF bytes
     * @param pageIndex 0-based page index to parse (default 0)
     * @return [GeoPdfParseResult] representing success, non-georeferenced, or error.
     */
    fun parse(inputStream: InputStream, pageIndex: Int = 0): GeoPdfParseResult
}
