package com.zirosaur.ziromaps.core.location.di

import android.content.Context
import android.hardware.SensorManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.FusedLocationProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
import com.zirosaur.ziromaps.core.location.provider.SensorCompassProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindCompassProvider(impl: SensorCompassProvider): CompassProvider

    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationClient(
            @ApplicationContext context: Context
        ): FusedLocationProviderClient {
            return LocationServices.getFusedLocationProviderClient(context)
        }

        @Provides
        @Singleton
        fun provideSensorManager(
            @ApplicationContext context: Context
        ): SensorManager {
            return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
    }
}
