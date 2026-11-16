package com.zirosaur.ziromaps.core.export

import com.zirosaur.ziromaps.core.export.kml.KmzExporter
import com.zirosaur.ziromaps.core.export.model.ExportDataset
import com.zirosaur.ziromaps.core.export.model.ExportTrack
import com.zirosaur.ziromaps.core.export.model.ExportTrackPoint
import com.zirosaur.ziromaps.core.export.model.ExportWaypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class KmzExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var exporter: KmzExporter
    private lateinit var outputFile: File
    private lateinit var dummyPhoto: File

    @Before
    fun setup() {
        exporter = KmzExporter()
        outputFile = File(tempFolder.root, "test_export.kmz")

        dummyPhoto = tempFolder.newFile("sample_field_photo.jpg")
        dummyPhoto.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x12, 0x34))
    }

    @Test
    fun exportToKmz_createsZipArchiveWithKmlAndImages() {
        val waypoints = listOf(
            ExportWaypoint(
                id = "wp-1",
                title = "WP-001",
                note = "Patok 01",
                latitude = -7.431250,
                longitude = 109.234510,
                altitudeM = 150.0,
                accuracyM = 4.0f,
                hasPhoto = false
            ),
            ExportWaypoint(
                id = "wp-2",
                title = "FOTO-001",
                note = "Pohon Bengkirai",
                latitude = -7.432000,
                longitude = 109.235000,
                altitudeM = 155.0,
                accuracyM = 3.5f,
                hasPhoto = true,
                photoFilePath = dummyPhoto.absolutePath,
                photoAzimuthDeg = 142.0f
            )
        )

        val tracks = listOf(
            ExportTrack(
                id = "tr-1",
                name = "Jalur Pantau",
                startedAtEpochMs = 1700000000000L,
                totalDistanceM = 1200.0,
                points = listOf(
                    ExportTrackPoint(-7.431250, 109.234510, 150.0, 4.0f, 1700000000000L),
                    ExportTrackPoint(-7.432000, 109.235000, 155.0, 3.5f, 1700001800000L)
                )
            )
        )

        val dataset = ExportDataset(
            projectName = "Cagar Alam X",
            waypoints = waypoints,
            tracks = tracks
        )

        val exported = exporter.exportToKmz(dataset, outputFile)
        assertTrue(exported.exists())
        assertTrue(exported.length() > 0)

        // Verify ZIP contents using ZipFile
        val zip = ZipFile(exported)
        val kmlEntry = zip.getEntry("doc.kml")
        assertNotNull(kmlEntry)

        val imageEntry = zip.getEntry("images/sample_field_photo.jpg")
        assertNotNull(imageEntry)
        assertEquals(dummyPhoto.length(), imageEntry.size)

        // Read KML text
        val kmlText = zip.getInputStream(kmlEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertTrue(kmlText.contains("<name><![CDATA[Cagar Alam X]]></name>"))
        assertTrue(kmlText.contains("<name><![CDATA[WP-001]]></name>"))
        assertTrue(kmlText.contains("<name><![CDATA[FOTO-001]]></name>"))
        assertTrue(kmlText.contains("<name><![CDATA[Jalur Pantau]]></name>"))
        assertTrue(kmlText.contains("<img src=\"images/sample_field_photo.jpg\""))
        assertTrue(kmlText.contains("<LineString>"))
        assertTrue(kmlText.contains("109.234510,-7.431250,150.0"))

        zip.close()
    }
}
