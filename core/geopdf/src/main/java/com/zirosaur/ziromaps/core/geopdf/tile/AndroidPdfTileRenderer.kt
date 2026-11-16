package com.zirosaur.ziromaps.core.geopdf.tile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class AndroidPdfTileRenderer @Inject constructor(
    private val memoryCache: TileMemoryCache,
    private val diskCache: TileDiskCache,
    @ApplicationContext private val context: Context
) : PdfTileRenderer {

    private val renderMutex = Mutex()

    override suspend fun renderTile(
        pdfFile: File,
        tile: MapTile,
        pageIndex: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext null

        // 1. Check in-memory LRU cache
        val memBitmap = memoryCache.get(tile.cacheKey)
        if (memBitmap != null && !memBitmap.isRecycled) {
            return@withContext memBitmap
        }

        // 2. Check persistent disk cache
        val diskBitmap = diskCache.get(tile)
        if (diskBitmap != null && !diskBitmap.isRecycled) {
            memoryCache.put(tile.cacheKey, diskBitmap)
            return@withContext diskBitmap
        }

        // 3. Render tile with thread-safe lock
        val renderedBitmap = renderMutex.withLock {
            // Check memory cache again in case another coroutine just rendered it
            val cachedAgain = memoryCache.get(tile.cacheKey)
            if (cachedAgain != null && !cachedAgain.isRecycled) {
                return@withLock cachedAgain
            }

            renderTileInternal(pdfFile, tile, pageIndex)
        } ?: return@withContext null

        // 4. Save to caches
        diskCache.put(tile, renderedBitmap)
        memoryCache.put(tile.cacheKey, renderedBitmap)

        return@withContext renderedBitmap
    }

    private fun renderTileInternal(
        pdfFile: File,
        tile: MapTile,
        pageIndex: Int
    ): Bitmap? {
        // Try hardware/native PdfRenderer first
        val nativeRendered = renderWithPdfRenderer(pdfFile, tile, pageIndex)
        if (nativeRendered != null) {
            return nativeRendered
        }

        // Fallback to PdfBox pure-graphics renderer (for headless/Robolectric or devices with pdfium quirks)
        return renderWithPdfBox(pdfFile, tile, pageIndex)
    }

    private fun renderWithPdfRenderer(
        pdfFile: File,
        tile: MapTile,
        pageIndex: Int
    ): Bitmap? {
        return try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY) ?: return null
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    renderer.openPage(pageIndex).use { page ->
                        val pagePixelWidth = page.width * tile.scale
                        val pagePixelHeight = page.height * tile.scale

                        val maxTilesX = ceil(pagePixelWidth / tile.tileSize).toInt()
                        val maxTilesY = ceil(pagePixelHeight / tile.tileSize).toInt()

                        if (tile.tileX < 0 || tile.tileX >= maxTilesX ||
                            tile.tileY < 0 || tile.tileY >= maxTilesY
                        ) {
                            return null
                        }

                        val bitmap = Bitmap.createBitmap(tile.tileSize, tile.tileSize, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)

                        val matrix = Matrix().apply {
                            postScale(tile.scale, tile.scale)
                            postTranslate(
                                -tile.tileX * tile.tileSize.toFloat(),
                                -tile.tileY * tile.tileSize.toFloat()
                            )
                        }

                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun renderWithPdfBox(
        pdfFile: File,
        tile: MapTile,
        pageIndex: Int
    ): Bitmap? {
        return try {
            if (!PDFBoxResourceLoader.isReady()) {
                PDFBoxResourceLoader.init(context)
            }

            PDDocument.load(pdfFile).use { doc ->
                if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return null
                val page = doc.getPage(pageIndex)
                val cropBox = page.cropBox ?: page.mediaBox

                val pagePixelWidth = (cropBox?.width ?: 0f) * tile.scale
                val pagePixelHeight = (cropBox?.height ?: 0f) * tile.scale

                val maxTilesX = ceil(pagePixelWidth / tile.tileSize).toInt()
                val maxTilesY = ceil(pagePixelHeight / tile.tileSize).toInt()

                if (tile.tileX < 0 || tile.tileX >= maxTilesX ||
                    tile.tileY < 0 || tile.tileY >= maxTilesY
                ) {
                    return null
                }

                val bitmap = Bitmap.createBitmap(tile.tileSize, tile.tileSize, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                val canvas = Canvas(bitmap)
                canvas.save()
                canvas.translate(
                    -tile.tileX * tile.tileSize.toFloat(),
                    -tile.tileY * tile.tileSize.toFloat()
                )

                val renderer = PDFRenderer(doc)
                renderer.renderPageToGraphics(pageIndex, Paint(), canvas, tile.scale, tile.scale)
                canvas.restore()

                bitmap
            }
        } catch (e: Throwable) {
            null
        }
    }

    override suspend fun clearCache(mapId: String) = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            memoryCache.clear()
            diskCache.clearMap(mapId)
        }
    }

    override suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            memoryCache.clear()
            diskCache.clearAll()
        }
    }
}
