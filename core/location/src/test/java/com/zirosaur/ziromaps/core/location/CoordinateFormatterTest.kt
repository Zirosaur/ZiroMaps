package com.zirosaur.ziromaps.core.location

import com.zirosaur.ziromaps.core.location.converter.CoordinateFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateFormatterTest {

    @Test
    fun toDecimalString_formatsCorrectly() {
        val lat = -7.431250
        val lon = 109.234510
        val formatted = CoordinateFormatter.toDecimalString(lat, lon)
        assertEquals("Lat: -7.431250°  Lon: 109.234510°", formatted)
    }

    @Test
    fun toDmsString_formatsCorrectly() {
        val lat = -7.431250
        val lon = 109.234510
        val dms = CoordinateFormatter.toDmsString(lat, lon)
        // -7.43125 deg = 7 deg, 25 min, 52.5 sec S
        // 109.23451 deg = 109 deg, 14 min, 04.2 sec E
        assertEquals("7°25'52.5\"S  109°14'04.2\"E", dms)
    }

    @Test
    fun toUtmString_formatsCorrectly() {
        val lat = -7.431250
        val lon = 109.234510
        val utmString = CoordinateFormatter.toUtmString(lat, lon)
        assertEquals("49M 305154m E 9178189m N", utmString)
    }
}
