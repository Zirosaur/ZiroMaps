package com.zirosaur.ziromaps.core.export

import com.zirosaur.ziromaps.core.export.csv.CsvExporter
import com.zirosaur.ziromaps.core.export.model.ExportDataset
import com.zirosaur.ziromaps.core.export.model.ExportTrack
import com.zirosaur.ziromaps.core.export.model.ExportTrackPoint
import com.zirosaur.ziromaps.core.export.model.ExportWaypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CsvExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var exporter: CsvExporter
    private lateinit var outputFile: File

    @Before
    fun setup() {
        exporter = CsvExporter()
        outputFile = File(tempFolder.root, "export.csv")
    }

    @Test
    fun exportToCsv_writesFormattedRows() {
        val waypoints = listOf(
            ExportWaypoint(
                id = "wp-1",
                title = "WP-001",
                note = "Titik aman, dekat pohon",
                latitude = -7.431250,
                longitude = 109.234510,
                altitudeM = 120.5,
                accuracyM = 4.0f,
                hasPhoto = false,
                createdAtEpochMs = 1700000000000L
            ),
            ExportWaypoint(
                id = "wp-2",
                title = "FOTO-001",
                note = "Foto pohon \"bengkirai\"",
                latitude = -7.432000,
                longitude = 109.235000,
                altitudeM = 125.0,
                accuracyM = 3.2f,
                hasPhoto = true,
                createdAtEpochMs = 1700000100000L,
                photoFilePath = "/photos/foto_1.jpg",
                photoAzimuthDeg = 142.5f
            )
        )

        val tracks = listOf(
            ExportTrack(
                id = "tr-1",
                name = "Track A",
                startedAtEpochMs = 1700000000000L,
                totalDistanceM = 500.0,
                points = listOf(
                    ExportTrackPoint(-7.431250, 109.234510, 120.0, 4.0f, 1700000000000L)
                )
            )
        )

        val dataset = ExportDataset(
            projectName = "Project Test",
            waypoints = waypoints,
            tracks = tracks
        )

        val exported = exporter.exportToCsv(dataset, outputFile)
        assertTrue(exported.exists())

        val lines = exported.readLines(Charsets.UTF_8)
        assertEquals(4, lines.size) // Header + 2 WPs + 1 Track Point

        // Header check
        assertEquals("ID,Name,Type,Latitude,Longitude,Altitude_m,Accuracy_m,Azimuth_deg,Note,Has_Photo,Photo_File,Created_At", lines[0])

        // Line 1: WP-001 (commas in note should be quoted)
        assertTrue(lines[1].contains("\"Titik aman, dekat pohon\""))
        assertTrue(lines[1].contains("-7.431250"))
        assertTrue(lines[1].contains("109.234510"))
        assertTrue(lines[1].contains("WAYPOINT"))

        // Line 2: FOTO-001 (escaped quotes in note)
        assertTrue(lines[2].contains("\"Foto pohon \"\"bengkirai\"\"\""))
        assertTrue(lines[2].contains("PHOTO"))
        assertTrue(lines[2].contains("foto_1.jpg"))
        assertTrue(lines[2].contains("142.5"))

        // Line 3: Track point
        assertTrue(lines[3].contains("TRACK_POINT"))
    }
}
