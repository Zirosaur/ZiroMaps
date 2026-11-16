package com.zirosaur.ziromaps.core.geopdf.model

/**
 * Represents a 2D point on a PDF page in user space units (points, 1/72 inch).
 * In standard PDF coordinate space, (0, 0) is the bottom-left corner of the page.
 */
data class PagePoint(
    val xPt: Double,
    val yPt: Double
)
