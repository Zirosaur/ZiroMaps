package com.zirosaur.ziromaps.core.export

import com.zirosaur.ziromaps.core.export.geojson.GeoJsonExporter
import com.zirosaur.ziromaps.core.export.model.ExportDataset
import com.zirosaur.ziromaps.core.export.model.ExportTrack
import com.zirosaur.ziromaps.core.export.model.ExportTrackPoint
import com.zirosaur.ziromaps.core.export.model.ExportWaypoint
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeoJsonExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var exporter: GeoJsonExporter
    private lateinit var outputFile: File

    @Before
    fun setup() {
        exporter = GeoJsonExporter()
        outputFile = File(tempFolder.root, "export.geojson")
    }

    @Test
    fun exportToGeoJson_createsValidFeatureCollection() {
        val waypoints = listOf(
            ExportWaypoint(
                id = "wp-1",
                title = "WP-001",
                note = "Patok Batas",
                latitude = -7.431250,
                longitude = 109.234510,
                altitudeM = 120.0,
                accuracyM = 4.2f,
                hasPhoto = false,
                createdAtEpochMs = 1700000000000L
            ),
            ExportWaypoint(
                id = "wp-2",
                title = "FOTO-001",
                note = "Pohon Jati",
                latitude = -7.432000,
                longitude = 109.235000,
                altitudeM = 125.0,
                accuracyM = 3.1f,
                hasPhoto = true,
                createdAtEpochMs = 1700000100000L,
                photoFilePath = "/photos/test.jpg",
                photoAzimuthDeg = 142.0f
            )
        )

        val tracks = listOf(
            ExportTrack(
                id = "tr-1",
                name = "Jalur Survey 1",
                startedAtEpochMs = 1700000000000L,
                endedAtEpochMs = 1700003600000L,
                totalDistanceM = 1500.0,
                points = listOf(
                    ExportTrackPoint(-7.431250, 109.234510, 120.0, 4.0f, 1700000000000L),
                    ExportTrackPoint(-7.432000, 109.235000, 125.0, 3.5f, 1700001800000L)
                )
            )
        )

        val dataset = ExportDataset(
            projectName = "Hutan Lindung A",
            waypoints = waypoints,
            tracks = tracks
        )

        val exported = exporter.exportToGeoJson(dataset, outputFile)
        assertTrue(exported.exists())

        val jsonString = exported.readText(Charsets.UTF_8)
        val json = JSONObject(jsonString)

        assertEquals("FeatureCollection", json.getString("type"))
        assertEquals("Hutan Lindung A", json.getString("name"))

        val features = json.getJSONArray("features")
        assertEquals(3, features.length())

        // Verify Point Feature 1
        val f1 = features.getJSONObject(0)
        assertEquals("Feature", f1.getString("type"))
        val g1 = f1.getJSONObject("geometry")
        assertEquals("Point", g1.getString("type"))
        val c1 = g1.getJSONArray("coordinates")
        assertEquals(109.234510, c1.getDouble(0), 0.0001) // Longitude first!
        assertEquals(-7.431250, c1.getDouble(1), 0.0001)  // Latitude second!
        assertEquals(120.0, c1.getDouble(2), 0.1)

        val p1 = f1.getJSONObject("properties")
        assertEquals("WP-001", p1.getString("name"))
        assertEquals("Patok Batas", p1.getString("note"))
        assertEquals(false, p1.getBoolean("hasPhoto"))

        // Verify LineString Feature
        val f3 = features.getJSONObject(2)
        val g3 = f3.getJSONObject("geometry")
        assertEquals("LineString", g3.getString("type"))
        val c3 = g3.getJSONArray("coordinates")
        assertEquals(2, c3.length())
    }
}
