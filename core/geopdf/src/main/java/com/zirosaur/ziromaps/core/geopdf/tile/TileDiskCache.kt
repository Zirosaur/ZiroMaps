package com.zirosaur.ziromaps.core.geopdf.tile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk persistent cache for compressed bitmap tiles.
 */
interface TileDiskCache {
    fun get(tile: MapTile): Bitmap?
    fun put(tile: MapTile, bitmap: Bitmap)
    fun remove(tile: MapTile)
    fun clearMap(mapId: String)
    fun clearAll()
}

@Singleton
class FileTileDiskCache @Inject constructor(
    @ApplicationContext private val context: Context
) : TileDiskCache {

    private val rootDir: File
        get() = File(context.cacheDir, "geopdf_tiles").apply { if (!exists()) mkdirs() }

    private fun getTileFile(tile: MapTile): File {
        val mapDir = File(rootDir, tile.mapId)
        val zoomDir = File(mapDir, tile.zoom.toString())
        return File(zoomDir, "${tile.tileX}_${tile.tileY}_s${tile.tileSize}.png")
    }

    override fun get(tile: MapTile): Bitmap? {
        val file = getTileFile(tile)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    override fun put(tile: MapTile, bitmap: Bitmap) {
        val file = getTileFile(tile)
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val tempFile = File(parent, "${file.name}.tmp_${System.currentTimeMillis()}")
        try {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            if (tempFile.exists()) {
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            tempFile.delete()
        }
    }

    override fun remove(tile: MapTile) {
        val file = getTileFile(tile)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun clearMap(mapId: String) {
        val mapDir = File(rootDir, mapId)
        if (mapDir.exists()) {
            mapDir.deleteRecursively()
        }
    }

    override fun clearAll() {
        if (rootDir.exists()) {
            rootDir.deleteRecursively()
        }
    }
}
