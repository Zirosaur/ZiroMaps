package com.zirosaur.ziromaps.core.export

import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.TrackPointEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import com.zirosaur.ziromaps.core.database.model.TrackWithPoints
import com.zirosaur.ziromaps.core.database.model.WaypointWithPhotos
import com.zirosaur.ziromaps.core.export.csv.CsvExporter
import com.zirosaur.ziromaps.core.export.geojson.GeoJsonExporter
import com.zirosaur.ziromaps.core.export.kml.KmzExporter
import com.zirosaur.ziromaps.core.export.model.ExportFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
class ExportManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var geoJsonExporter: GeoJsonExporter
    private lateinit var csvExporter: CsvExporter
    private lateinit var kmzExporter: KmzExporter
    private lateinit var mapProjectDao: MapProjectDao
    private lateinit var waypointDao: WaypointDao
    private lateinit var trackDao: TrackDao

    private lateinit var exportManager: ExportManagerImpl

    @Before
    fun setup() {
        geoJsonExporter = GeoJsonExporter()
        csvExporter = CsvExporter()
        kmzExporter = KmzExporter()
        mapProjectDao = mockk(relaxed = true)
        waypointDao = mockk(relaxed = true)
        trackDao = mockk(relaxed = true)

        exportManager = ExportManagerImpl(
            geoJsonExporter = geoJsonExporter,
            csvExporter = csvExporter,
            kmzExporter = kmzExporter,
            mapProjectDao = mapProjectDao,
            waypointDao = waypointDao,
            trackDao = trackDao
        )
    }

    @Test
    fun exportProject_geoJson_createsValidFile(): Unit = runBlocking {
        val dummyProject = MapProjectEntity(
            id = "proj-123",
            name = "Gunung Slamet",
            pdfLocalPath = "/path/slamet.pdf",
            pageWidthPt = 1000.0,
            pageHeightPt = 1000.0,
            geoStandard = "ISO_32000",
            epsgCode = 32749,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1700000000000L
        )
        coEvery { mapProjectDao.getProjectByIdSync("proj-123") } returns dummyProject

        val waypoints = listOf(
            WaypointWithPhotos(
                waypoint = WaypointEntity(
                    id = "wp-1",
                    projectId = "proj-123",
                    title = "Pos 1",
                    note = "Mata air",
                    latitude = -7.2,
                    longitude = 109.2,
                    altitudeM = 1500.0,
                    accuracyM = 3.0f,
                    hasPhoto = false,
                    createdAtEpochMs = 1700000000000L
                ),
                photos = emptyList()
            )
        )
        every { waypointDao.getWaypointsWithPhotosForProject("proj-123") } returns flowOf(waypoints)

        val tracks = listOf(
            TrackWithPoints(
                track = TrackEntity(
                    id = "tr-1",
                    projectId = "proj-123",
                    name = "Jalur Bambangan",
                    startedAtEpochMs = 1700000000000L,
                    endedAtEpochMs = 1700010000000L,
                    totalDistanceM = 3200.0
                ),
                points = listOf(
                    TrackPointEntity(1L, "tr-1", -7.2, 109.2, 1500.0, 3.0f, 1700000000000L)
                )
            )
        )
        every { trackDao.getTracksWithPointsForProject("proj-123") } returns flowOf(tracks)

        val destDir = tempFolder.newFolder("exports")
        val result = exportManager.exportProject("proj-123", ExportFormat.GEOJSON, destDir)

        assertTrue(result.isSuccess)
        val file = result.getOrNull()
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertEquals("Gunung_Slamet_export.geojson", file.name)
        assertTrue(file.readText().contains("Gunung Slamet"))
    }

    @Test
    fun exportProject_csv_createsValidFile(): Unit = runBlocking {
        val dummyProject = MapProjectEntity(
            id = "proj-123",
            name = "Taman Nasional",
            pdfLocalPath = "/path/test.pdf",
            pageWidthPt = 1000.0,
            pageHeightPt = 1000.0,
            geoStandard = "ISO_32000",
            epsgCode = 32749,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1700000000000L
        )
        coEvery { mapProjectDao.getProjectByIdSync("proj-123") } returns dummyProject
        every { waypointDao.getWaypointsWithPhotosForProject("proj-123") } returns flowOf(emptyList())
        every { trackDao.getTracksWithPointsForProject("proj-123") } returns flowOf(emptyList())

        val destDir = tempFolder.newFolder("exports_csv")
        val result = exportManager.exportProject("proj-123", ExportFormat.CSV, destDir)

        assertTrue(result.isSuccess)
        val file = result.getOrNull()
        assertNotNull(file)
        assertTrue(file!!.name.endsWith(".csv"))
    }

    @Test
    fun exportProject_kmz_createsValidFile(): Unit = runBlocking {
        val dummyProject = MapProjectEntity(
            id = "proj-123",
            name = "Taman Nasional",
            pdfLocalPath = "/path/test.pdf",
            pageWidthPt = 1000.0,
            pageHeightPt = 1000.0,
            geoStandard = "ISO_32000",
            epsgCode = 32749,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1700000000000L
        )
        coEvery { mapProjectDao.getProjectByIdSync("proj-123") } returns dummyProject
        every { waypointDao.getWaypointsWithPhotosForProject("proj-123") } returns flowOf(emptyList())
        every { trackDao.getTracksWithPointsForProject("proj-123") } returns flowOf(emptyList())

        val destDir = tempFolder.newFolder("exports_kmz")
        val result = exportManager.exportProject("proj-123", ExportFormat.KMZ, destDir)

        assertTrue(result.isSuccess)
        val file = result.getOrNull()
        assertNotNull(file)
        assertTrue(file!!.name.endsWith(".kmz"))
    }
}
