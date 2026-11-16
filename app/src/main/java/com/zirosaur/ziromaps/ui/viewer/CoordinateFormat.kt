package com.zirosaur.ziromaps.ui.viewer

/**
 * Display format for geographical and projection coordinates in the viewer status bar.
 */
enum class CoordinateFormat {
    UTM,
    DECIMAL,
    DMS;

    fun next(): CoordinateFormat = when (this) {
        UTM -> DECIMAL
        DECIMAL -> DMS
        DMS -> UTM
    }
}
