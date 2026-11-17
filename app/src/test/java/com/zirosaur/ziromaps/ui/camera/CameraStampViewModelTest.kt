package com.zirosaur.ziromaps.ui.camera

import android.content.Context
import android.graphics.Bitmap
import com.zirosaur.ziromaps.core.camera.CameraXManager
import com.zirosaur.ziromaps.core.camera.model.CameraState
import com.zirosaur.ziromaps.core.camera.model.CapturedImage
import com.zirosaur.ziromaps.core.camera.model.FlashMode
import com.zirosaur.ziromaps.core.camera.model.LensFacing
import com.zirosaur.ziromaps.core.camera.watermark.PhotoProcessor
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkConfig
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkPosition
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.PhotoRecordDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.PhotoRecordEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import com.zirosaur.ziromaps.core.location.model.CompassData
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
import com.zirosaur.ziromaps.data.preferences.StampSettingsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CameraStampViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cameraXManager: CameraXManager
    private lateinit var photoProcessor: PhotoProcessor
    private lateinit var locationProvider: LocationProvider
    private lateinit var compassProvider: CompassProvider
    private lateinit var mapProjectDao: MapProjectDao
    private lateinit var waypointDao: WaypointDao
    private lateinit var photoRecordDao: PhotoRecordDao
    private lateinit var stampSettingsManager: StampSettingsManager
    private lateinit var context: Context

    private val cameraStateFlow = MutableStateFlow<CameraState>(CameraState.Ready)
    private val flashModeFlow = MutableStateFlow(FlashMode.OFF)
    private val lensFacingFlow = MutableStateFlow(LensFacing.BACK)
    private val zoomRatioFlow = MutableStateFlow(1f)
    private val minZoomRatioFlow = MutableStateFlow(1f)
    private val maxZoomRatioFlow = MutableStateFlow(5f)
    private val locationFlow = MutableSharedFlow<UserLocation>(replay = 1)
    private val compassFlow = MutableSharedFlow<CompassData>(replay = 1)

    private lateinit var viewModel: CameraStampViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        cameraXManager = mockk(relaxed = true) {
            every { cameraState } returns cameraStateFlow
            every { flashMode } returns flashModeFlow
            every { lensFacing } returns lensFacingFlow
            every { zoomRatio } returns zoomRatioFlow
            every { minZoomRatio } returns minZoomRatioFlow
            every { maxZoomRatio } returns maxZoomRatioFlow
        }
        photoProcessor = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true) {
            every { getLocationUpdates() } returns locationFlow
        }
        compassProvider = mockk(relaxed = true) {
            every { getCompassUpdates() } returns compassFlow
        }
        mapProjectDao = mockk(relaxed = true)
        waypointDao = mockk(relaxed = true)
        photoRecordDao = mockk(relaxed = true)
        stampSettingsManager = mockk(relaxed = true) {
            every { loadConfig() } returns WatermarkConfig()
            every { getLastPhotoPath() } returns null
        }
        context = mockk(relaxed = true) {
            every { filesDir } returns tempFolder.root
        }

        val dummyProject = MapProjectEntity(
            id = "proj-1",
            name = "Test Forest Map",
            pdfLocalPath = "/path/test.pdf",
            pageWidthPt = 1000.0,
            pageHeightPt = 1000.0,
            geoStandard = "ISO_32000",
            epsgCode = 32749,
            transformMatrixJson = "{}",
            createdAtEpochMs = 1700000000000L
        )
        every { mapProjectDao.getProjectById("proj-1") } returns flowOf(dummyProject)

        viewModel = CameraStampViewModel(
            cameraXManager = cameraXManager,
            photoProcessor = photoProcessor,
            locationProvider = locationProvider,
            compassProvider = compassProvider,
            mapProjectDao = mapProjectDao,
            waypointDao = waypointDao,
            photoRecordDao = photoRecordDao,
            stampSettingsManager = stampSettingsManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setProjectId_loadsProjectName() = runTest(testDispatcher) {
        viewModel.setProjectId("proj-1")
        advanceUntilIdle()

        assertEquals("Test Forest Map", viewModel.uiState.value.projectName)
    }

    @Test
    fun locationAndCompassUpdates_reflectInUiState() = runTest(testDispatcher) {
        val mockLocation = UserLocation(
            latitude = -7.431250,
            longitude = 109.234510,
            accuracyM = 4.5f,
            altitudeM = 120.0
        )
        locationFlow.emit(mockLocation)

        val mockCompass = CompassData(azimuthDeg = 142f)
        compassFlow.emit(mockCompass)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(-7.431250, state.currentLocation?.latitude ?: 0.0, 0.0001)
        assertEquals(109.234510, state.currentLocation?.longitude ?: 0.0, 0.0001)
        assertNotNull(state.currentUtm)
        assertEquals(142f, state.currentCompass?.azimuthDeg)
        assertTrue(state.isGpsFixed)
        assertTrue(state.isHighAccuracy)
    }

    @Test
    fun cameraControls_delegatesToManager() {
        viewModel.toggleFlashMode()
        verify { cameraXManager.toggleFlashMode() }

        viewModel.setZoomRatio(2.0f)
        verify { cameraXManager.setZoomRatio(2.0f) }

        viewModel.updateQuickNote("Pohon jati nomor 4")
        assertEquals("Pohon jati nomor 4", viewModel.uiState.value.quickNote)
    }

    @Test
    fun takePhoto_success_createsWaypointAndPhotoRecord() = runTest(testDispatcher) {
        viewModel.setProjectId("proj-1")

        val mockLocation = UserLocation(
            latitude = -7.431250,
            longitude = 109.234510,
            accuracyM = 4.5f,
            altitudeM = 150.0
        )
        locationFlow.emit(mockLocation)
        compassFlow.emit(CompassData(azimuthDeg = 142f))
        viewModel.updateQuickNote("Pohon Uji")
        advanceUntilIdle()

        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val capturedImage = CapturedImage(
            bitmap = mockBitmap,
            rotationDegrees = 0,
            width = 1920,
            height = 1080
        )
        coEvery { cameraXManager.capturePhoto() } returns Result.success(capturedImage)
        coEvery { photoProcessor.processAndSavePhoto(any(), any(), any(), any()) } answers {
            Result.success(thirdArg())
        }
        coEvery { waypointDao.getWaypointCountForProject("proj-1") } returns 0

        val wpSlot = slot<WaypointEntity>()
        coEvery { waypointDao.insert(capture(wpSlot)) } returns Unit

        val photoSlot = slot<PhotoRecordEntity>()
        coEvery { photoRecordDao.insert(capture(photoSlot)) } returns Unit

        viewModel.takePhoto(context)
        advanceUntilIdle()

        // Verify WaypointEntity
        coVerify(exactly = 1) { waypointDao.insert(any()) }
        assertTrue(wpSlot.isCaptured)
        val savedWp = wpSlot.captured
        assertEquals("FOTO-001", savedWp.title)
        assertEquals("Pohon Uji", savedWp.note)
        assertEquals(-7.431250, savedWp.latitude, 0.0001)
        assertEquals(109.234510, savedWp.longitude, 0.0001)
        assertTrue(savedWp.hasPhoto)

        // Verify PhotoRecordEntity
        coVerify(exactly = 1) { photoRecordDao.insert(any()) }
        assertTrue(photoSlot.isCaptured)
        val savedPhoto = photoSlot.captured
        assertEquals(savedWp.id, savedPhoto.waypointId)
        assertEquals(142f, savedPhoto.azimuthDeg)
        assertTrue(savedPhoto.filePath.endsWith(".jpg"))

        // Verify UiState
        assertEquals(savedWp.id, viewModel.uiState.value.photoSavedWaypointId)
        assertEquals(false, viewModel.uiState.value.isSavingPhoto)
    }

    @Test
    fun takePhoto_captureFailure_setsErrorState() = runTest(testDispatcher) {
        viewModel.setProjectId("proj-1")
        coEvery { cameraXManager.capturePhoto() } returns Result.failure(RuntimeException("Camera hardware error"))

        viewModel.takePhoto(context)
        advanceUntilIdle()

        assertEquals("Camera hardware error", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isSavingPhoto)
        coVerify(exactly = 0) { waypointDao.insert(any()) }
    }

    @Test
    fun watermarkConfigUpdatesPersistAndReflectInUiState() = runTest(testDispatcher) {
        val newConfig = WatermarkConfig(
            position = WatermarkPosition.TOP_LEFT,
            showAltitude = false,
            showUtm = false
        )
        viewModel.updateWatermarkConfig(newConfig)

        assertEquals(WatermarkPosition.TOP_LEFT, viewModel.uiState.value.watermarkConfig.position)
        assertFalse(viewModel.uiState.value.watermarkConfig.showAltitude)
        verify { stampSettingsManager.saveConfig(newConfig) }

        viewModel.openStampSettings()
        assertTrue(viewModel.uiState.value.showStampSettings)

        viewModel.dismissStampSettings()
        assertFalse(viewModel.uiState.value.showStampSettings)
    }
}
