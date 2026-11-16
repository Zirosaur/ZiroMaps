package com.zirosaur.ziromaps.core.location.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.zirosaur.ziromaps.core.location.model.UserLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FusedLocationProvider @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : LocationProvider {

    private fun hasPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(intervalMs: Long): Flow<UserLocation> = callbackFlow {
        if (!hasPermission()) {
            close(SecurityException("ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is required"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(0f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    val userLocation = UserLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitudeM = if (loc.hasAltitude()) loc.altitude else null,
                        accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                        speedMps = if (loc.hasSpeed()) loc.speed else null,
                        bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                        timestampEpochMs = loc.time
                    )
                    trySend(userLocation)
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): UserLocation? = suspendCancellableCoroutine { cont ->
        if (!hasPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedClient.lastLocation
            .addOnSuccessListener(Runnable::run) { loc ->
                if (loc != null) {
                    cont.resume(
                        UserLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitudeM = if (loc.hasAltitude()) loc.altitude else null,
                            accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                            speedMps = if (loc.hasSpeed()) loc.speed else null,
                            bearingDeg = if (loc.hasBearing()) loc.bearing else null,
                            timestampEpochMs = loc.time
                        )
                    )
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener(Runnable::run) {
                cont.resume(null)
            }
    }
}
