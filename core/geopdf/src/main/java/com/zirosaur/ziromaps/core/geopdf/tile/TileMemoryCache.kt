package com.zirosaur.ziromaps.core.geopdf.tile

import android.graphics.Bitmap
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory LRU cache for decoded bitmap tiles.
 */
interface TileMemoryCache {
    fun get(key: String): Bitmap?
    fun put(key: String, bitmap: Bitmap)
    fun remove(key: String): Bitmap?
    fun clear()
    fun size(): Int
    fun maxSize(): Int
}

@Singleton
class LruTileMemoryCache @Inject constructor() : TileMemoryCache {

    private val cache: LruCache<String, Bitmap>

    init {
        // Allocate up to 1/8th of available runtime memory, clamped between 16 MB and 64 MB
        val maxMemoryBytes = Runtime.getRuntime().maxMemory()
        val cacheSizeBytes = (maxMemoryBytes / 8).toInt().coerceIn(16 * 1024 * 1024, 64 * 1024 * 1024)

        cache = object : LruCache<String, Bitmap>(cacheSizeBytes) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount.coerceAtLeast(1)
            }
        }
    }

    override fun get(key: String): Bitmap? = cache.get(key)

    override fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    override fun remove(key: String): Bitmap? = cache.remove(key)

    override fun clear() {
        cache.evictAll()
    }

    override fun size(): Int = cache.size()

    override fun maxSize(): Int = cache.maxSize()
}
