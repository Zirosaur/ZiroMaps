package com.zirosaur.ziromaps.ui.viewer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import com.zirosaur.ziromaps.core.database.model.TrackWithPoints
import com.zirosaur.ziromaps.core.database.model.WaypointWithPhotos
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.geopdf.tile.PdfTileRenderer
import com.zirosaur.ziromaps.core.location.model.CompassData
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
import com.zirosaur.ziromaps.service.TrackRecordingManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mapProjectDao = mockk<MapProjectDao>()
    private val waypointDao = mockk<WaypointDao>(relaxed = true)
    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val locationProvider = mockk<LocationProvider>()
    private val compassProvider = mockk<CompassProvider>()
    private val tileRenderer = mockk<PdfTileRenderer>()
    private val trackRecordingManager = TrackRecordingManager()

    private val projectFlow = MutableStateFlow<MapProjectEntity?>(null)
    private val waypointsFlow = MutableStateFlow<List<WaypointWithPhotos>>(emptyList())
    private val tracksFlow = MutableStateFlow<List<TrackWithPoints>>(emptyList())
    private val locationFlow = MutableSharedFlow<UserLocation>(replay = 1)
    private val compassFlow = MutableSharedFlow<CompassData>(replay = 1)

    private val testMatrix = TransformMatrix(
        c0 = 109.0, c1 = 0.0001, c2 = 0.0,
        d0 = -7.0, d1 = 0.0, d2 = -0.0001,
        e0 = -1090000.0, e1 = 10000.0, e2 = 0.0,
        f0 = -70000.0, f1 = 0.0, f2 = -10000.0,
        boundsLatLon = listOf(
            LatLon(-7.0, 109.0),
            LatLon(-7.0, 109.1),
            LatLon(-7.1, 109.1),
            LatLon(-7.1, 109.0)
        ),
        bboxPagePoints = listOf(
            PagePoint(0.0, 0.0),
            PagePoint(1000.0, 0.0),
            PagePoint(1000.0, 1000.0),
            PagePoint(0.0, 1000.0)
        )
    )

    private val testProject = MapProjectEntity(
        id = "test-map-123",
        name = "Central Java Map",
        pdfLocalPath = "/data/maps/test.pdf",
        pageWidthPt = 1000.0,
        pageHeightPt = 1000.0,
        geoStandard = "ISO_32000",
        epsgCode = 32749,
        transformMatrixJson = testMatrix.toJson(),
        createdAtEpochMs = 5000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mapProjectDao.getProjectById("test-map-123") } returns projectFlow
        every { waypointDao.getWaypointsWithPhotosForProject("test-map-123") } returns waypointsFlow
        every { trackDao.getTracksWithPointsForProject("test-map-123") } returns tracksFlow
        every { locationProvider.getLocationUpdates(any()) } returns locationFlow
        every { compassProvider.getCompassUpdates() } returns compassFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MapViewerViewModel {
        val savedState = SavedStateHandle(mapOf("projectId" to "test-map-123"))
        return MapViewerViewModel(
            savedStateHandle = savedState,
            mapProjectDao = mapProjectDao,
            waypointDao = waypointDao,
            trackDao = trackDao,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            trackRecordingManager = trackRecordingManager,
            tileRenderer = tileRenderer
        )
    }

    @Test
    fun loadsProjectAndParsesTransformMatrix() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.mapProject)
            assertEquals("Central Java Map", state.mapProject?.name)
            assertNotNull(state.transformMatrix)
            assertEquals(109.0, state.transformMatrix?.c0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun togglesCoordinateFormatInOrder() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertEquals(CoordinateFormat.UTM, viewModel.uiState.value.coordinateFormat)

        viewModel.toggleCoordinateFormat()
        assertEquals(CoordinateFormat.DECIMAL, viewModel.uiState.value.coordinateFormat)

        viewModel.toggleCoordinateFormat()
        assertEquals(CoordinateFormat.DMS, viewModel.uiState.value.coordinateFormat)

        viewModel.toggleCoordinateFormat()
        assertEquals(CoordinateFormat.UTM, viewModel.uiState.value.coordinateFormat)
    }

    @Test
    fun locationUpdatesCalculatesUtmAndBoundsAccurately() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        val insideLoc = UserLocation(
            latitude = -7.05,
            longitude = 109.05,
            altitudeM = 15.0,
            accuracyM = 4.2f,
            speedMps = 1.0f,
            bearingDeg = 45f,
            timestampEpochMs = 123456L
        )
        locationFlow.emit(insideLoc)
        testDispatcher.scheduler.advanceUntilIdle()

        val stateInside = viewModel.uiState.value
        assertNotNull(stateInside.currentLocation)
        assertEquals(-7.05, stateInside.currentLocation?.latitude ?: 0.0, 0.001)
        assertNotNull(stateInside.currentUtm)
        assertEquals(49, stateInside.currentUtm?.zone)
        assertTrue(stateInside.isGpsInsideBounds)
        assertNotNull(stateInside.userPagePoint)

        val outsideLoc = UserLocation(
            latitude = -7.5,
            longitude = 109.5,
            altitudeM = 10.0,
            accuracyM = 5.0f,
            speedMps = 0f,
            bearingDeg = null,
            timestampEpochMs = 123500L
        )
        locationFlow.emit(outsideLoc)
        testDispatcher.scheduler.advanceUntilIdle()

        val stateOutside = viewModel.uiState.value
        assertFalse(stateOutside.isGpsInsideBounds)
    }

    @Test
    fun compassUpdatesReflectInUiState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        compassFlow.emit(CompassData(azimuthDeg = 135f, cardinalDirection = "SE"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(135f, viewModel.uiState.value.currentCompass?.azimuthDeg)
        assertEquals("SE", viewModel.uiState.value.currentCompass?.cardinalDirection)
    }

    @Test
    fun centerOnGpsBehaviors() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.centerOnGps()
        assertTrue(viewModel.uiState.value.userNotice?.contains("Menunggu") == true)
        assertEquals(0L, viewModel.uiState.value.centerGpsTrigger)

        val outsideLoc = UserLocation(-7.8, 109.8, null, 10f, null, null, 1000L)
        locationFlow.emit(outsideLoc)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.centerOnGps()
        assertTrue(viewModel.uiState.value.userNotice?.contains("luar") == true)
        assertEquals(0L, viewModel.uiState.value.centerGpsTrigger)

        val insideLoc = UserLocation(-7.05, 109.05, null, 5f, null, null, 1000L)
        locationFlow.emit(insideLoc)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.centerOnGps()
        assertTrue(viewModel.uiState.value.centerGpsTrigger > 0L)
    }

    @Test
    fun waypointCrudOperations() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Open and Dismiss Dialog
        viewModel.openAddWaypointDialog()
        assertTrue(viewModel.uiState.value.showAddWaypointDialog)

        viewModel.dismissAddWaypointDialog()
        assertFalse(viewModel.uiState.value.showAddWaypointDialog)

        // 2. Save Waypoint
        coEvery { waypointDao.insert(any()) } returns Unit
        viewModel.saveWaypoint(
            title = "Pos Pantau 1",
            note = "Dekat jembatan",
            latitude = -7.05,
            longitude = 109.05,
            altitudeM = 50.0,
            accuracyM = 3.5f
        )
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            waypointDao.insert(match {
                it.projectId == "test-map-123" &&
                        it.title == "Pos Pantau 1" &&
                        it.note == "Dekat jembatan" &&
                        it.latitude == -7.05 &&
                        it.longitude == 109.05
            })
        }
        assertTrue(viewModel.uiState.value.userNotice?.contains("Pos Pantau 1") == true)

        // 3. Selection & Detail Sheet
        val wpEntity = WaypointEntity(
            id = "wp-100",
            projectId = "test-map-123",
            title = "Pos Pantau 1",
            note = "Dekat jembatan",
            latitude = -7.05,
            longitude = 109.05,
            altitudeM = 50.0,
            accuracyM = 3.5f,
            hasPhoto = false,
            createdAtEpochMs = 12345L
        )
        val wpWithPhotos = WaypointWithPhotos(waypoint = wpEntity, photos = emptyList())
        waypointsFlow.value = listOf(wpWithPhotos)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.waypoints.size)

        viewModel.selectWaypoint(wpWithPhotos)
        assertEquals("wp-100", viewModel.uiState.value.selectedWaypoint?.waypoint?.id)

        // 4. Update Waypoint
        coEvery { waypointDao.getWaypointByIdSync("wp-100") } returns wpEntity
        coEvery { waypointDao.update(any()) } returns Unit

        viewModel.updateWaypoint("wp-100", "Pos Pantau Baru", "Catatan revisi")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            waypointDao.update(match {
                it.id == "wp-100" && it.title == "Pos Pantau Baru" && it.note == "Catatan revisi"
            })
        }

        // 5. Delete Waypoint
        coEvery { waypointDao.deleteById("wp-100") } returns Unit
        viewModel.deleteWaypoint("wp-100")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { waypointDao.deleteById("wp-100") }
        assertNull(viewModel.uiState.value.selectedWaypoint)
        assertTrue(viewModel.uiState.value.userNotice?.contains("dihapus") == true)
    }

    @Test
    fun tracksObservationAndRecordingState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        val sampleTrack = TrackWithPoints(
            track = TrackEntity(
                id = "track-1",
                projectId = "test-map-123",
                name = "Jalur Survey",
                startedAtEpochMs = 1000L,
                endedAtEpochMs = null,
                totalDistanceM = 1500.0
            ),
            points = emptyList()
        )
        tracksFlow.value = listOf(sampleTrack)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.tracks.size)
        assertEquals("Jalur Survey", viewModel.uiState.value.tracks[0].track.name)

        // Track recording updates via TrackRecordingManager
        trackRecordingManager.onTrackStarted("track-1", "test-map-123")
        trackRecordingManager.onPointRecorded(1500.0, 30)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRecordingTrack)
        assertEquals(1500.0, viewModel.uiState.value.recordedDistanceM, 0.01)

        trackRecordingManager.onTrackStopped()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRecordingTrack)
    }
}
