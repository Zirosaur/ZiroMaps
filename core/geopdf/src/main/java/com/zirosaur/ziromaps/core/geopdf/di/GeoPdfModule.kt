package com.zirosaur.ziromaps.core.geopdf.di

import com.zirosaur.ziromaps.core.geopdf.parser.CompositeGeoPdfParser
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParser
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
}
