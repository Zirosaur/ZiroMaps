package com.zirosaur.ziromaps.core.export.di

import com.zirosaur.ziromaps.core.export.ExportManager
import com.zirosaur.ziromaps.core.export.ExportManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @Singleton
    abstract fun bindExportManager(
        impl: ExportManagerImpl
    ): ExportManager
}
