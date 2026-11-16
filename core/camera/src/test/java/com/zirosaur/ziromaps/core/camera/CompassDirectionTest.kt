package com.zirosaur.ziromaps.core.camera

import com.zirosaur.ziromaps.core.camera.watermark.CompassDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassDirectionTest {

    @Test
    fun toCardinal_cardinalDirections() {
        assertEquals("N", CompassDirection.toCardinal(0f))
        assertEquals("N", CompassDirection.toCardinal(15f))
        assertEquals("NE", CompassDirection.toCardinal(45f))
        assertEquals("E", CompassDirection.toCardinal(90f))
        assertEquals("SE", CompassDirection.toCardinal(135f))
        assertEquals("SE", CompassDirection.toCardinal(142f))
        assertEquals("S", CompassDirection.toCardinal(180f))
        assertEquals("SW", CompassDirection.toCardinal(225f))
        assertEquals("W", CompassDirection.toCardinal(270f))
        assertEquals("NW", CompassDirection.toCardinal(315f))
        assertEquals("N", CompassDirection.toCardinal(355f))
        assertEquals("N", CompassDirection.toCardinal(360f))
    }

    @Test
    fun toCardinal_negativeAngles() {
        assertEquals("W", CompassDirection.toCardinal(-90f))
        assertEquals("S", CompassDirection.toCardinal(-180f))
        assertEquals("E", CompassDirection.toCardinal(-270f))
    }

    @Test
    fun formatAzimuth_formatsCorrectly() {
        assertEquals("142° SE", CompassDirection.formatAzimuth(142f))
        assertEquals("0° N", CompassDirection.formatAzimuth(0f))
        assertEquals("90° E", CompassDirection.formatAzimuth(90.4f))
        assertEquals("-", CompassDirection.formatAzimuth(null))
    }
}
