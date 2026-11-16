package com.zirosaur.ziromaps.core.geopdf.di

import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParser
import com.zirosaur.ziromaps.core.geopdf.parser.Iso32000Parser
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
        impl: Iso32000Parser
    ): GeoPdfParser
}
