package com.zirosaur.ziromaps.core.geopdf

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.zirosaur.ziromaps.core.geopdf.tile.AndroidPdfTileRenderer
import com.zirosaur.ziromaps.core.geopdf.tile.FileTileDiskCache
import com.zirosaur.ziromaps.core.geopdf.tile.LruTileMemoryCache
import com.zirosaur.ziromaps.core.geopdf.tile.MapTile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfTileRendererTest {

    private lateinit var context: Context
    private lateinit var memoryCache: LruTileMemoryCache
    private lateinit var diskCache: FileTileDiskCache
    private lateinit var tileRenderer: AndroidPdfTileRenderer

    private fun getSamplePdfFile(): File {
        val candidates = listOf(
            File("sample-data/BKPH RAWA TMR.pdf"),
            File("../../sample-data/BKPH RAWA TMR.pdf"),
            File("../sample-data/BKPH RAWA TMR.pdf"),
            File(System.getProperty("user.dir") ?: ".", "sample-data/BKPH RAWA TMR.pdf")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not find sample-data/BKPH RAWA TMR.pdf")
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        memoryCache = LruTileMemoryCache()
        diskCache = FileTileDiskCache(context)
        tileRenderer = AndroidPdfTileRenderer(
            memoryCache = memoryCache,
            diskCache = diskCache,
            context = context
        )
    }

    @After
    fun tearDown() = runBlocking {
        tileRenderer.clearAllCache()
    }

    @Test
    fun renderTile_sampleMap_returnsValid512x512Bitmap() = runBlocking {
        val pdfFile = getSamplePdfFile()
        val tile = MapTile(
            mapId = "test_map_1",
            zoom = 0,
            tileX = 0,
            tileY = 0,
            tileSize = 512
        )

        val bitmap = tileRenderer.renderTile(pdfFile, tile)
        assertNotNull("Rendered tile should not be null", bitmap)
        assertEquals(512, bitmap!!.width)
        assertEquals(512, bitmap.height)
    }

    @Test
    fun renderTile_outOfBounds_returnsNull() = runBlocking {
        val pdfFile = getSamplePdfFile()
        // At zoom 0, tiles are roughly 5x5. (100, 100) is far beyond page limits
        val outOfBoundsTile = MapTile(
            mapId = "test_map_1",
            zoom = 0,
            tileX = 100,
            tileY = 100,
            tileSize = 512
        )

        val bitmap = tileRenderer.renderTile(pdfFile, outOfBoundsTile)
        assertNull("Out of bounds tile should return null", bitmap)
    }

    @Test
    fun renderTile_memoryAndDiskCachePipeline() = runBlocking {
        val pdfFile = getSamplePdfFile()
        val tile = MapTile(
            mapId = "test_map_cache",
            zoom = 1,
            tileX = 0,
            tileY = 0,
            tileSize = 512
        )

        // 1. First render: cache miss -> renders and saves to both caches
        assertNull(memoryCache.get(tile.cacheKey))
        assertNull(diskCache.get(tile))

        val bitmap1 = tileRenderer.renderTile(pdfFile, tile)
        assertNotNull(bitmap1)

        // Verify both caches are now populated
        assertNotNull(memoryCache.get(tile.cacheKey))
        assertNotNull(diskCache.get(tile))

        // 2. Second render: hit from memory cache
        val bitmap2 = tileRenderer.renderTile(pdfFile, tile)
        assertNotNull(bitmap2)
        assertEquals(bitmap1, bitmap2)

        // 3. Clear memory cache only: next render should load from disk cache
        memoryCache.clear()
        assertNull(memoryCache.get(tile.cacheKey))
        assertNotNull(diskCache.get(tile))

        val bitmap3 = tileRenderer.renderTile(pdfFile, tile)
        assertNotNull(bitmap3)
        // Memory cache should be re-populated from disk
        assertNotNull(memoryCache.get(tile.cacheKey))
    }

    @Test
    fun clearCache_removesAllCachedTilesForMap() = runBlocking {
        val pdfFile = getSamplePdfFile()
        val tile = MapTile(
            mapId = "map_to_clear",
            zoom = 0,
            tileX = 0,
            tileY = 0
        )

        val bitmap = tileRenderer.renderTile(pdfFile, tile)
        assertNotNull(bitmap)
        assertNotNull(diskCache.get(tile))

        tileRenderer.clearCache("map_to_clear")
        assertNull(memoryCache.get(tile.cacheKey))
        assertNull(diskCache.get(tile))
    }

    @Test
    fun memoryCache_putAndGet() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val key = "custom_tile_key"

        memoryCache.put(key, bitmap)
        val retrieved = memoryCache.get(key)
        assertNotNull(retrieved)
        assertEquals(bitmap, retrieved)

        memoryCache.remove(key)
        assertNull(memoryCache.get(key))
    }

    @Test
    fun diskCache_putGetAndRemove() {
        val tile = MapTile("map_disk_test", 2, 3, 4)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        diskCache.put(tile, bitmap)
        val loaded = diskCache.get(tile)
        assertNotNull(loaded)
        assertEquals(64, loaded!!.width)
        assertEquals(64, loaded.height)

        diskCache.remove(tile)
        assertNull(diskCache.get(tile))
    }
}
