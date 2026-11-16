package com.zirosaur.ziromaps.core.camera

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.zirosaur.ziromaps.core.camera.model.DeviceOrientation
import com.zirosaur.ziromaps.core.camera.orientation.DeviceOrientationListener
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceOrientationListenerTest {

    private lateinit var app: Application
    private lateinit var orientationListener: DeviceOrientationListener

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        orientationListener = DeviceOrientationListener(app)
    }

    @Test
    fun defaultOrientation_isPortraitUp() {
        assertEquals(DeviceOrientation.PORTRAIT_UP, orientationListener.orientation.value)
        assertEquals(0, orientationListener.rawAngle.value)
    }

    @Test
    fun setOrientationForTesting_updatesStateFlow() {
        orientationListener.setOrientationForTesting(DeviceOrientation.LANDSCAPE_RIGHT)
        assertEquals(DeviceOrientation.LANDSCAPE_RIGHT, orientationListener.orientation.value)
        assertEquals(90, orientationListener.rawAngle.value)

        orientationListener.setOrientationForTesting(DeviceOrientation.PORTRAIT_DOWN)
        assertEquals(DeviceOrientation.PORTRAIT_DOWN, orientationListener.orientation.value)
        assertEquals(180, orientationListener.rawAngle.value)

        orientationListener.setOrientationForTesting(DeviceOrientation.LANDSCAPE_LEFT)
        assertEquals(DeviceOrientation.LANDSCAPE_LEFT, orientationListener.orientation.value)
        assertEquals(270, orientationListener.rawAngle.value)
    }

    @Test
    fun startAndStop_safeToCall() {
        orientationListener.start()
        orientationListener.stop()
    }
}
