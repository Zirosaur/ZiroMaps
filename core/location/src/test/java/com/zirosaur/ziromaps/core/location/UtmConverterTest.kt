package com.zirosaur.ziromaps.core.location

import com.zirosaur.ziromaps.core.location.converter.UtmConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

class UtmConverterTest {

    @Test
    fun latLonToUtm_centralJavaSample_producesAccurateZoneAndCoordinates() {
        // Sample location in Central Java (Banyumas / Cilacap area)
        val lat = -7.431250
        val lon = 109.234510

        val utm = UtmConverter.latLonToUtm(lat, lon)

        assertEquals(49, utm.zone)
        assertEquals('M', utm.band)
        assertTrue(utm.isSouth)

        // Expected Easting ~ 305154m, Northing ~ 9178189m
        assertEquals(305154.0, utm.eastingM, 1.0)
        assertEquals(9178189.0, utm.northingM, 1.0)

        // Formatted string matching SSOT spec
        val formatted = utm.toFormattedString()
        assertEquals("49M 305154m E 9178189m N", formatted)
    }

    @Test
    fun utmRoundTrip_subMillimeterAccuracyGlobally() {
        val testPoints = listOf(
            Pair(-7.431250, 109.234510), // Central Java (Zone 49S, M)
            Pair(-7.787350, 108.800270), // Sample Map BKPH Rawa corner (Zone 49S, M)
            Pair(0.0, 100.0),            // Equator (Zone 47N, N)
            Pair(48.856600, 2.352200),   // Paris (Zone 31N, U)
            Pair(40.712800, -74.006000), // New York (Zone 18N, T)
            Pair(-33.868800, 151.209300),// Sydney (Zone 56S, H)
            Pair(51.507400, -0.127800)   // London (Zone 30N, U)
        )

        for ((lat, lon) in testPoints) {
            val utm = UtmConverter.latLonToUtm(lat, lon)
            val (recLat, recLon) = UtmConverter.utmToLatLon(utm)

            val dLat = abs(lat - recLat)
            val dLon = abs(lon - recLon)

            // Convert degree error to distance in meters (1 deg lat ~ 111,000m)
            val errM = sqrt(
                (dLat * 111000.0) * (dLat * 111000.0) +
                        (dLon * 111000.0 * cos(Math.toRadians(lat))) * (dLon * 111000.0 * cos(Math.toRadians(lat)))
            )

            // Sub-millimeter error target (< 0.001 meters)
            assertTrue("Round-trip error at ($lat, $lon) is too high: $errM m", errM < 0.001)
        }
    }

    @Test
    fun getZone_correctlyIdentifiesZonesAcrossMeridians() {
        assertEquals(31, UtmConverter.getZone(0.0))   // Prime Meridian (Zone 31: 0° to 6°E)
        assertEquals(30, UtmConverter.getZone(-0.1))  // Just West of Greenwich (Zone 30)
        assertEquals(1, UtmConverter.getZone(-180.0)) // Date Line West
        assertEquals(60, UtmConverter.getZone(179.9)) // Date Line East
        assertEquals(49, UtmConverter.getZone(109.5)) // Java (108° to 114°E)
    }

    @Test
    fun getBand_correctlyMapsLatitudeLetters() {
        assertEquals('M', UtmConverter.getBand(-7.5))  // Central Java (-8° to 0°)
        assertEquals('N', UtmConverter.getBand(0.0))   // Equator (0° to 8°)
        assertEquals('C', UtmConverter.getBand(-75.0)) // Far South (-80° to -72°)
        assertEquals('X', UtmConverter.getBand(80.0))  // Far North (72° to 84°)
        assertEquals('U', UtmConverter.getBand(50.0))  // Central Europe (48° to 56°)
    }
}
