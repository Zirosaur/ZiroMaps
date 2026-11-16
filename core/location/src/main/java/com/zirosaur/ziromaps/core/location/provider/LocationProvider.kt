package com.zirosaur.ziromaps.core.location.provider

import com.zirosaur.ziromaps.core.location.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Service providing user GPS / network location updates.
 */
interface LocationProvider {
    /**
     * Emits continuous location updates as a cold [Flow].
     *
     * @param intervalMs requested interval between location fixes (default 1000 ms)
     */
    fun getLocationUpdates(intervalMs: Long = 1000L): Flow<UserLocation>

    /**
     * Returns the most recent cached location fix, or null if none is available.
     */
    suspend fun getLastKnownLocation(): UserLocation?
}
