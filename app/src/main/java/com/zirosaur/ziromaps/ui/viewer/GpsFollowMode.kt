package com.zirosaur.ziromaps.ui.viewer

/**
 * GPS follow mode on the interactive map canvas.
 */
enum class GpsFollowMode {
    /** Map pans freely without auto-centering on GPS. */
    FREE,

    /** Map continuously centers on current GPS position. */
    FOLLOW_LOCATION,

    /** Map continuously centers on GPS and rotates according to compass azimuth. */
    HEADING;

    fun next(): GpsFollowMode = when (this) {
        FREE -> FOLLOW_LOCATION
        FOLLOW_LOCATION -> HEADING
        HEADING -> FREE
    }
}
