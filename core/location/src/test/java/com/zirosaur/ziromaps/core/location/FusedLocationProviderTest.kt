package com.zirosaur.ziromaps.core.location

import android.Manifest
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Tasks
import com.zirosaur.ziromaps.core.location.provider.FusedLocationProvider
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FusedLocationProviderTest {

    private lateinit var app: Application
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationProvider: FusedLocationProvider

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        fusedClient = mockk(relaxed = true)
        locationProvider = FusedLocationProvider(fusedClient, app)
    }

    @Test(expected = SecurityException::class)
    fun getLocationUpdates_permissionDenied_throwsSecurityException(): Unit = runBlocking {
        Shadows.shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        locationProvider.getLocationUpdates().first()
    }

    @Test
    fun getLocationUpdates_permissionGranted_emitsLocation(): Unit = runBlocking {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val callbackSlot = slot<LocationCallback>()
        every {
            fusedClient.requestLocationUpdates(any<LocationRequest>(), capture(callbackSlot), any<Looper>())
        } returns Tasks.forResult(null)

        val mockLocation = Location("gps").apply {
            latitude = -7.431250
            longitude = 109.234510
            altitude = 120.0
            accuracy = 4.2f
            time = 1700000000000L
        }

        locationProvider.getLocationUpdates().test {
            assertTrue(callbackSlot.isCaptured)
            callbackSlot.captured.onLocationResult(LocationResult.create(listOf(mockLocation)))

            val received = awaitItem()
            assertEquals(-7.431250, received.latitude, 1e-6)
            assertEquals(109.234510, received.longitude, 1e-6)
            assertEquals(120.0, received.altitudeM ?: 0.0, 0.1)
            assertEquals(4.2f, received.accuracyM ?: 0f, 0.1f)
            cancelAndIgnoreRemainingEvents()
        }

        verify { fusedClient.removeLocationUpdates(callbackSlot.captured) }
    }

    @Test
    fun getLastKnownLocation_permissionGranted_returnsCachedLocation(): Unit = runBlocking {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        val mockLocation = Location("gps").apply {
            latitude = -7.5
            longitude = 109.0
            time = 1700000000000L
        }
        every { fusedClient.lastLocation } returns Tasks.forResult(mockLocation)

        val result = locationProvider.getLastKnownLocation()
        assertNotNull(result)
        assertEquals(-7.5, result!!.latitude, 1e-6)
        assertEquals(109.0, result.longitude, 1e-6)
    }

    @Test
    fun getLastKnownLocation_permissionDenied_returnsNull(): Unit = runBlocking {
        Shadows.shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val result = locationProvider.getLastKnownLocation()
        assertNull(result)
    }
}
