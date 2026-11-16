package com.zirosaur.ziromaps.core.camera

import android.view.Surface
import com.zirosaur.ziromaps.core.camera.model.DeviceOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceOrientationTest {

    @Test
    fun fromDegrees_portraitUpAngles_returnsPortraitUp() {
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(0))
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(20))
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(44))
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(315))
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(359))
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(360))
        assertEquals(DeviceOrientation.PORTRAIT_UP, DeviceOrientation.fromDegrees(720))
    }

    @Test
    fun fromDegrees_landscapeRightAngles_returnsLandscapeRight() {
        assertEquals(DeviceOrientation.LANDSCAPE_RIGHT, DeviceOrientation.fromDegrees(45))
        assertEquals(DeviceOrientation.LANDSCAPE_RIGHT, DeviceOrientation.fromDegrees(90))
        assertEquals(DeviceOrientation.LANDSCAPE_RIGHT, DeviceOrientation.fromDegrees(134))
    }

    @Test
    fun fromDegrees_portraitDownAngles_returnsPortraitDown() {
        assertEquals(DeviceOrientation.PORTRAIT_DOWN, DeviceOrientation.fromDegrees(135))
        assertEquals(DeviceOrientation.PORTRAIT_DOWN, DeviceOrientation.fromDegrees(180))
        assertEquals(DeviceOrientation.PORTRAIT_DOWN, DeviceOrientation.fromDegrees(224))
    }

    @Test
    fun fromDegrees_landscapeLeftAngles_returnsLandscapeLeft() {
        assertEquals(DeviceOrientation.LANDSCAPE_LEFT, DeviceOrientation.fromDegrees(225))
        assertEquals(DeviceOrientation.LANDSCAPE_LEFT, DeviceOrientation.fromDegrees(270))
        assertEquals(DeviceOrientation.LANDSCAPE_LEFT, DeviceOrientation.fromDegrees(314))
    }

    @Test
    fun fromDegrees_negativeAngles_handledCorrectly() {
        assertEquals(DeviceOrientation.LANDSCAPE_LEFT, DeviceOrientation.fromDegrees(-90))
        assertEquals(DeviceOrientation.PORTRAIT_DOWN, DeviceOrientation.fromDegrees(-180))
        assertEquals(DeviceOrientation.LANDSCAPE_RIGHT, DeviceOrientation.fromDegrees(-270))
    }

    @Test
    fun surfaceRotation_matchesAndroidStandards() {
        assertEquals(Surface.ROTATION_0, DeviceOrientation.PORTRAIT_UP.surfaceRotation)
        assertEquals(Surface.ROTATION_90, DeviceOrientation.LANDSCAPE_RIGHT.surfaceRotation)
        assertEquals(Surface.ROTATION_180, DeviceOrientation.PORTRAIT_DOWN.surfaceRotation)
        assertEquals(Surface.ROTATION_270, DeviceOrientation.LANDSCAPE_LEFT.surfaceRotation)
    }
}
