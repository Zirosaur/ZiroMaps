package com.zirosaur.ziromaps.core.geopdf.di

import com.zirosaur.ziromaps.core.geopdf.parser.CompositeGeoPdfParser
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParser
import com.zirosaur.ziromaps.core.geopdf.tile.AndroidPdfTileRenderer
import com.zirosaur.ziromaps.core.geopdf.tile.FileTileDiskCache
import com.zirosaur.ziromaps.core.geopdf.tile.LruTileMemoryCache
import com.zirosaur.ziromaps.core.geopdf.tile.PdfTileRenderer
import com.zirosaur.ziromaps.core.geopdf.tile.TileDiskCache
import com.zirosaur.ziromaps.core.geopdf.tile.TileMemoryCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GeoPdfModule {

    @Binds
    @Singleton
    abstract fun bindGeoPdfParser(
        impl: CompositeGeoPdfParser
    ): GeoPdfParser

    @Binds
    @Singleton
    abstract fun bindPdfTileRenderer(
        impl: AndroidPdfTileRenderer
    ): PdfTileRenderer

    @Binds
    @Singleton
    abstract fun bindTileMemoryCache(
        impl: LruTileMemoryCache
    ): TileMemoryCache

    @Binds
    @Singleton
    abstract fun bindTileDiskCache(
        impl: FileTileDiskCache
    ): TileDiskCache
}
