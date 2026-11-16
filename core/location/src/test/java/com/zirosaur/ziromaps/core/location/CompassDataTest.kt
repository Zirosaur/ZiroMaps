package com.zirosaur.ziromaps.core.location

import com.zirosaur.ziromaps.core.location.model.CompassData
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassDataTest {

    @Test
    fun getCardinal_mapsAccuratelyAcrossDirections() {
        assertEquals("N", CompassData.getCardinal(0f))
        assertEquals("N", CompassData.getCardinal(359f))
        assertEquals("NE", CompassData.getCardinal(45f))
        assertEquals("E", CompassData.getCardinal(90f))
        assertEquals("SE", CompassData.getCardinal(135f))
        assertEquals("SE", CompassData.getCardinal(142f))
        assertEquals("S", CompassData.getCardinal(180f))
        assertEquals("SW", CompassData.getCardinal(225f))
        assertEquals("W", CompassData.getCardinal(270f))
        assertEquals("NW", CompassData.getCardinal(315f))
    }

    @Test
    fun toFormattedString_formatsProperly() {
        val compass = CompassData(142.4f)
        assertEquals("142° SE", compass.toFormattedString())
    }
}
