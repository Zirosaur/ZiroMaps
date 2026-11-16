package com.zirosaur.ziromaps.ui.viewer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.geopdf.tile.PdfTileRenderer
import com.zirosaur.ziromaps.core.location.model.CompassData
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
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
    private val locationProvider = mockk<LocationProvider>()
    private val compassProvider = mockk<CompassProvider>()
    private val tileRenderer = mockk<PdfTileRenderer>()

    private val projectFlow = MutableStateFlow<MapProjectEntity?>(null)
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
        every { locationProvider.getLocationUpdates(any()) } returns locationFlow
        every { compassProvider.getCompassUpdates() } returns compassFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsProjectAndParsesTransformMatrix() = runTest(testDispatcher) {
        val savedState = SavedStateHandle(mapOf("projectId" to "test-map-123"))
        val viewModel = MapViewerViewModel(
            savedStateHandle = savedState,
            mapProjectDao = mapProjectDao,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            tileRenderer = tileRenderer
        )

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
        val savedState = SavedStateHandle(mapOf("projectId" to "test-map-123"))
        val viewModel = MapViewerViewModel(
            savedStateHandle = savedState,
            mapProjectDao = mapProjectDao,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            tileRenderer = tileRenderer
        )

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
        val savedState = SavedStateHandle(mapOf("projectId" to "test-map-123"))
        val viewModel = MapViewerViewModel(
            savedStateHandle = savedState,
            mapProjectDao = mapProjectDao,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            tileRenderer = tileRenderer
        )

        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        // Inside polygon: (-7.05, 109.05)
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

        // Outside polygon: (-7.5, 109.5)
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
        val savedState = SavedStateHandle(mapOf("projectId" to "test-map-123"))
        val viewModel = MapViewerViewModel(
            savedStateHandle = savedState,
            mapProjectDao = mapProjectDao,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            tileRenderer = tileRenderer
        )
        testDispatcher.scheduler.advanceUntilIdle()

        compassFlow.emit(CompassData(azimuthDeg = 135f, cardinalDirection = "SE"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(135f, viewModel.uiState.value.currentCompass?.azimuthDeg)
        assertEquals("SE", viewModel.uiState.value.currentCompass?.cardinalDirection)
    }

    @Test
    fun centerOnGpsBehaviors() = runTest(testDispatcher) {
        val savedState = SavedStateHandle(mapOf("projectId" to "test-map-123"))
        val viewModel = MapViewerViewModel(
            savedStateHandle = savedState,
            mapProjectDao = mapProjectDao,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            tileRenderer = tileRenderer
        )
        projectFlow.value = testProject
        testDispatcher.scheduler.advanceUntilIdle()

        // Case 1: GPS is null
        viewModel.centerOnGps()
        assertTrue(viewModel.uiState.value.userNotice?.contains("Menunggu") == true)
        assertEquals(0L, viewModel.uiState.value.centerGpsTrigger)

        // Case 2: GPS is outside bounds
        val outsideLoc = UserLocation(-7.8, 109.8, null, 10f, null, null, 1000L)
        locationFlow.emit(outsideLoc)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.centerOnGps()
        assertTrue(viewModel.uiState.value.userNotice?.contains("luar") == true)
        assertEquals(0L, viewModel.uiState.value.centerGpsTrigger)

        // Case 3: GPS is inside bounds
        val insideLoc = UserLocation(-7.05, 109.05, null, 5f, null, null, 1000L)
        locationFlow.emit(insideLoc)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.centerOnGps()
        assertTrue(viewModel.uiState.value.centerGpsTrigger > 0L)
    }
}
