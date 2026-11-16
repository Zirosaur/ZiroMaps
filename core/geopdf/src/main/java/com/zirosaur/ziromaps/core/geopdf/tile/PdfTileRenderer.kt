package com.zirosaur.ziromaps.core.geopdf.tile

import android.graphics.Bitmap
import java.io.File

/**
 * Service responsible for rendering and caching PDF map tiles.
 */
interface PdfTileRenderer {
    /**
     * Renders or retrieves a cached bitmap tile for the specified [tile].
     *
     * @param pdfFile the source PDF file on disk
     * @param tile the tile specification (mapId, zoom, tileX, tileY, tileSize, scale)
     * @param pageIndex 0-based PDF page index (default 0)
     * @return the rendered [Bitmap] or null if the tile is outside page bounds or file is missing
     */
    suspend fun renderTile(
        pdfFile: File,
        tile: MapTile,
        pageIndex: Int = 0
    ): Bitmap?

    /**
     * Clears all memory and disk cached tiles for the specified [mapId].
     */
    suspend fun clearCache(mapId: String)

    /**
     * Clears all cached tiles across all maps.
     */
    suspend fun clearAllCache()
}
