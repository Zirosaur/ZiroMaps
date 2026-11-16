package com.zirosaur.ziromaps.core.camera.di

import com.zirosaur.ziromaps.core.camera.CameraXManager
import com.zirosaur.ziromaps.core.camera.CameraXManagerImpl
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
}
