package com.zirosaur.ziromaps.core.location.provider

import com.zirosaur.ziromaps.core.location.model.CompassData
import kotlinx.coroutines.flow.Flow

/**
 * Service providing real-time compass heading / azimuth readings.
 */
interface CompassProvider {
    /**
     * Emits continuous azimuth updates as a cold [Flow].
     */
    fun getCompassUpdates(): Flow<CompassData>
}
