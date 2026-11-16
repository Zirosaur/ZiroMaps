package com.zirosaur.ziromaps.core.camera.di

import com.zirosaur.ziromaps.core.camera.CameraXManager
import com.zirosaur.ziromaps.core.camera.CameraXManagerImpl
import com.zirosaur.ziromaps.core.camera.watermark.CanvasWatermarkRenderer
import com.zirosaur.ziromaps.core.camera.watermark.ExifMetadataWriter
import com.zirosaur.ziromaps.core.camera.watermark.ExifMetadataWriterImpl
import com.zirosaur.ziromaps.core.camera.watermark.PhotoProcessor
import com.zirosaur.ziromaps.core.camera.watermark.PhotoProcessorImpl
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkRenderer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraXManager(
        impl: CameraXManagerImpl
    ): CameraXManager

    @Binds
    @Singleton
    abstract fun bindWatermarkRenderer(
        impl: CanvasWatermarkRenderer
    ): WatermarkRenderer

    @Binds
    @Singleton
    abstract fun bindExifMetadataWriter(
        impl: ExifMetadataWriterImpl
    ): ExifMetadataWriter

    @Binds
    @Singleton
    abstract fun bindPhotoProcessor(
        impl: PhotoProcessorImpl
    ): PhotoProcessor
}
