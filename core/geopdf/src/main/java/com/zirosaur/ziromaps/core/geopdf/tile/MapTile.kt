package com.zirosaur.ziromaps.core.geopdf.tile

import kotlin.math.pow

/**
 * Identifies a single square tile in the map pyramid.
 *
 * @param mapId unique identifier of the map project
 * @param zoom integer zoom level (e.g. 0 to 6)
 * @param tileX horizontal tile index (0 to tilesX - 1)
 * @param tileY vertical tile index (0 to tilesY - 1)
 * @param tileSize width and height of the tile in pixels (default 512)
 * @param scale rendering scale factor relative to PDF points (1/72 inch)
 */
data class MapTile(
    val mapId: String,
    val zoom: Int,
    val tileX: Int,
    val tileY: Int,
    val tileSize: Int = DEFAULT_TILE_SIZE,
    val scale: Float = calculateScale(zoom)
) {
    val cacheKey: String get() = "${mapId}_z${zoom}_x${tileX}_y${tileY}_s$tileSize"

    companion object {
        const val DEFAULT_TILE_SIZE = 512

        /**
         * Calculates rendering scale factor for a given zoom level.
         * Zoom 0: 0.25x (overview)
         * Zoom 1: 0.50x
         * Zoom 2: 1.00x (1 pt = 1 px, native 72 DPI)
         * Zoom 3: 2.00x (144 DPI)
         * Zoom 4: 4.00x (288 DPI)
         * Zoom 5: 8.00x (576 DPI)
         */
        fun calculateScale(zoom: Int): Float {
            return 2.0.pow(zoom - 2).toFloat()
        }
    }
}
