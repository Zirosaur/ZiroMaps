package com.zirosaur.ziromaps.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.PhotoRecordDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.PhotoRecordEntity
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.TrackPointEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomCrudTest {

    private lateinit var db: AppDatabase
    private lateinit var projectDao: MapProjectDao
    private lateinit var waypointDao: WaypointDao
    private lateinit var photoDao: PhotoRecordDao
    private lateinit var trackDao: TrackDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = db.mapProjectDao()
        waypointDao = db.waypointDao()
        photoDao = db.photoRecordDao()
        trackDao = db.trackDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetMapProject() = runTest {
        val project = MapProjectEntity(
            id = "proj-123",
            name = "BKPH RAWA TMR",
            pdfLocalPath = "/data/user/0/com.zirosaur.ziromaps/files/maps/proj-123/source.pdf",
            pageWidthPt = 595.0,
            pageHeightPt = 842.0,
            geoStandard = "ISO_32000",
            epsgCode = 4326,
            transformMatrixJson = "{\"a\":1.0,\"b\":0.0,\"c\":0.0,\"d\":1.0,\"tx\":0.0,\"ty\":0.0}",
            createdAtEpochMs = 1731670000000L
        )

        projectDao.insert(project)

        val retrieved = projectDao.getProjectByIdSync("proj-123")
        assertNotNull(retrieved)
        assertEquals("BKPH RAWA TMR", retrieved?.name)
        assertEquals(4326, retrieved?.epsgCode)
        assertEquals("ISO_32000", retrieved?.geoStandard)

        projectDao.getAllProjects().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("proj-123", list[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDelete_whenProjectDeleted_waypointsAndTracksAreDeleted() = runTest {
        val project = MapProjectEntity(
            id = "proj-cascade",
            name = "Cascade Test Map",
            pdfLocalPath = "/files/source.pdf",
            pageWidthPt = 500.0,
            pageHeightPt = 500.0,
            geoStandard = "OGC_LGIDICT",
            epsgCode = 32749,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1000L
        )
        projectDao.insert(project)

        val waypoint = WaypointEntity(
            id = "wp-1",
            projectId = "proj-cascade",
            title = "WP-001",
            note = "Observation post",
            latitude = -7.12345,
            longitude = 110.12345,
            altitudeM = 120.5,
            accuracyM = 3.5f,
            hasPhoto = true,
            createdAtEpochMs = 2000L
        )
        waypointDao.insert(waypoint)

        val photo = PhotoRecordEntity(
            id = "photo-1",
            waypointId = "wp-1",
            filePath = "/files/photo1.jpg",
            azimuthDeg = 180.0f,
            capturedAtEpochMs = 2100L
        )
        photoDao.insert(photo)

        val track = TrackEntity(
            id = "track-1",
            projectId = "proj-cascade",
            name = "Jalur Patroli",
            startedAtEpochMs = 3000L,
            endedAtEpochMs = 4000L,
            totalDistanceM = 1540.0
        )
        trackDao.insertTrack(track)

        val trackPoint = TrackPointEntity(
            id = 0,
            trackId = "track-1",
            latitude = -7.12345,
            longitude = 110.12345,
            altitudeM = 120.0,
            accuracyM = 4.0f,
            recordedAtEpochMs = 3100L
        )
        trackDao.insertTrackPoint(trackPoint)

        // Verify entities exist
        assertNotNull(projectDao.getProjectByIdSync("proj-cascade"))
        assertNotNull(waypointDao.getWaypointByIdSync("wp-1"))
        assertNotNull(photoDao.getPhotoByIdSync("photo-1"))
        assertNotNull(trackDao.getTrackByIdSync("track-1"))

        // Delete parent project
        projectDao.deleteById("proj-cascade")

        // Verify cascade deletion
        assertNull(projectDao.getProjectByIdSync("proj-cascade"))
        assertNull(waypointDao.getWaypointByIdSync("wp-1"))
        assertNull(photoDao.getPhotoByIdSync("photo-1"))
        assertNull(trackDao.getTrackByIdSync("track-1"))

        trackDao.getTrackPoints("track-1").test {
            val points = awaitItem()
            assertTrue(points.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun waypointWithPhotos_relationQueriesCorrectly() = runTest {
        val project = MapProjectEntity(
            id = "proj-rel",
            name = "Relation Map",
            pdfLocalPath = "/files/source.pdf",
            pageWidthPt = 600.0,
            pageHeightPt = 800.0,
            geoStandard = "ISO_32000",
            epsgCode = 4326,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1000L
        )
        projectDao.insert(project)

        val waypoint = WaypointEntity(
            id = "wp-rel-1",
            projectId = "proj-rel",
            title = "Tree Sample 1",
            note = "Diameter 45cm",
            latitude = -7.5,
            longitude = 109.5,
            altitudeM = 50.0,
            accuracyM = 2.0f,
            hasPhoto = true,
            createdAtEpochMs = 2000L
        )
        waypointDao.insert(waypoint)

        val photo1 = PhotoRecordEntity(
            id = "photo-101",
            waypointId = "wp-rel-1",
            filePath = "/photos/p1.jpg",
            azimuthDeg = 90.0f,
            capturedAtEpochMs = 2001L
        )
        val photo2 = PhotoRecordEntity(
            id = "photo-102",
            waypointId = "wp-rel-1",
            filePath = "/photos/p2.jpg",
            azimuthDeg = 270.0f,
            capturedAtEpochMs = 2002L
        )
        photoDao.insertAll(listOf(photo1, photo2))

        waypointDao.getWaypointWithPhotos("wp-rel-1").test {
            val item = awaitItem()
            assertNotNull(item)
            assertEquals("wp-rel-1", item?.waypoint?.id)
            assertEquals(2, item?.photos?.size)
            assertEquals("photo-101", item?.photos?.get(0)?.id)
            assertEquals(90.0f, item?.photos?.get(0)?.azimuthDeg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun trackWithPoints_relationQueriesCorrectly() = runTest {
        val project = MapProjectEntity(
            id = "proj-track",
            name = "Track Map",
            pdfLocalPath = "/files/source.pdf",
            pageWidthPt = 400.0,
            pageHeightPt = 400.0,
            geoStandard = "ISO_32000",
            epsgCode = null,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1000L
        )
        projectDao.insert(project)

        val track = TrackEntity(
            id = "track-survey",
            projectId = "proj-track",
            name = "Survey Track",
            startedAtEpochMs = 10000L,
            endedAtEpochMs = 20000L,
            totalDistanceM = 850.25
        )
        trackDao.insertTrack(track)

        val p1 = TrackPointEntity(trackId = "track-survey", latitude = -7.0, longitude = 109.0, altitudeM = 10.0, accuracyM = 3.0f, recordedAtEpochMs = 10100L)
        val p2 = TrackPointEntity(trackId = "track-survey", latitude = -7.001, longitude = 109.001, altitudeM = 11.0, accuracyM = 3.2f, recordedAtEpochMs = 10200L)
        trackDao.insertTrackPoints(listOf(p1, p2))

        trackDao.getTrackWithPoints("track-survey").test {
            val trackWithPoints = awaitItem()
            assertNotNull(trackWithPoints)
            assertEquals("Survey Track", trackWithPoints?.track?.name)
            assertEquals(2, trackWithPoints?.points?.size)
            assertEquals(-7.0, trackWithPoints?.points?.get(0)?.latitude ?: 0.0, 0.00001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
