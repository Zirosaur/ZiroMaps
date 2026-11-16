package com.zirosaur.ziromaps.domain

/**
 * Thrown when an imported PDF does not contain supported georeferencing metadata.
 */
class NotGeoPdfException(
    message: String = "File PDF tidak memiliki metadata georeferensi yang didukung."
) : Exception(message)

/**
 * Thrown when an error occurs while parsing or copying the PDF file.
 */
class GeoPdfParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
