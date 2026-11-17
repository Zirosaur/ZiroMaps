package com.zirosaur.ziromaps.ui.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirosaur.ziromaps.core.camera.CameraXManager
import com.zirosaur.ziromaps.core.camera.watermark.PhotoProcessor
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkConfig
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkData
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.PhotoRecordDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.PhotoRecordEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import com.zirosaur.ziromaps.core.location.converter.UtmConverter
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
import com.zirosaur.ziromaps.data.preferences.StampSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CameraStampViewModel @Inject constructor(
    val cameraXManager: CameraXManager,
    private val photoProcessor: PhotoProcessor,
    private val locationProvider: LocationProvider,
    private val compassProvider: CompassProvider,
    private val mapProjectDao: MapProjectDao,
    private val waypointDao: WaypointDao,
    private val photoRecordDao: PhotoRecordDao,
    private val stampSettingsManager: StampSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraStampUiState())
    val uiState: StateFlow<CameraStampUiState> = _uiState.asStateFlow()

    init {
        // Load persisted settings
        val persistedConfig = stampSettingsManager.loadConfig()
        val lastPhoto = stampSettingsManager.getLastPhotoPath()
        _uiState.update {
            it.copy(
                watermarkConfig = persistedConfig,
                lastCapturedPhotoPath = lastPhoto
            )
        }

        observeCameraManager()
        observeLocationAndCompass()
    }

    private fun observeCameraManager() {
        viewModelScope.launch {
            cameraXManager.cameraState.collect { state ->
                _uiState.update { it.copy(cameraState = state) }
            }
        }
        viewModelScope.launch {
            cameraXManager.flashMode.collect { mode ->
                _uiState.update { it.copy(flashMode = mode) }
            }
        }
        viewModelScope.launch {
            cameraXManager.lensFacing.collect { facing ->
                _uiState.update { it.copy(lensFacing = facing) }
            }
        }
        viewModelScope.launch {
            cameraXManager.zoomRatio.collect { zoom ->
                _uiState.update { it.copy(zoomRatio = zoom) }
            }
        }
        viewModelScope.launch {
            cameraXManager.minZoomRatio.collect { minZ ->
                _uiState.update { it.copy(minZoomRatio = minZ) }
            }
        }
        viewModelScope.launch {
            cameraXManager.maxZoomRatio.collect { maxZ ->
                _uiState.update { it.copy(maxZoomRatio = maxZ) }
            }
        }
    }

    private fun observeLocationAndCompass() {
        viewModelScope.launch {
            locationProvider.getLocationUpdates()
                .catch { /* ignore in blank spots */ }
                .collect { location ->
                    val utm = UtmConverter.latLonToUtm(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            currentLocation = location,
                            currentUtm = utm
                        )
                    }
                }
        }

        viewModelScope.launch {
            compassProvider.getCompassUpdates()
                .catch { /* ignore sensor errors */ }
                .collect { compass ->
                    _uiState.update { it.copy(currentCompass = compass) }
                }
        }
    }

    fun setProjectId(projectId: String) {
        if (_uiState.value.projectId == projectId) return
        _uiState.update { it.copy(projectId = projectId) }

        viewModelScope.launch {
            mapProjectDao.getProjectById(projectId).collect { project ->
                if (project != null) {
                    _uiState.update { it.copy(projectName = project.name) }
                }
            }
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
    }

    fun updateQuickNote(note: String) {
        _uiState.update { it.copy(quickNote = note) }
    }

    fun updateWatermarkConfig(config: WatermarkConfig) {
        stampSettingsManager.saveConfig(config)
        _uiState.update { it.copy(watermarkConfig = config) }
    }

    fun openStampSettings() {
        _uiState.update { it.copy(showStampSettings = true) }
    }

    fun dismissStampSettings() {
        _uiState.update { it.copy(showStampSettings = false) }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraXManager.bindToLifecycle(lifecycleOwner, previewView, _uiState.value.lensFacing)
    }

    fun unbindCamera() {
        cameraXManager.unbind()
    }

    fun toggleFlashMode() {
        cameraXManager.toggleFlashMode()
    }

    fun toggleLensFacing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraXManager.toggleLensFacing(lifecycleOwner, previewView)
    }

    fun setZoomRatio(ratio: Float) {
        cameraXManager.setZoomRatio(ratio)
    }

    fun focusAndMeter(x: Float, y: Float, previewView: PreviewView) {
        cameraXManager.focusAndMeter(x, y, previewView)
    }

    fun takePhoto(context: Context) {
        if (_uiState.value.isSavingPhoto) return

        val projectId = _uiState.value.projectId
        if (projectId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ID Proyek tidak valid") }
            return
        }

        _uiState.update { it.copy(isSavingPhoto = true, errorMessage = null) }

        viewModelScope.launch {
            val captureResult = cameraXManager.capturePhoto()
            if (captureResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isSavingPhoto = false,
                        errorMessage = captureResult.exceptionOrNull()?.message ?: "Gagal mengambil foto"
                    )
                }
                return@launch
            }

            val capturedImage = captureResult.getOrThrow()
            val state = _uiState.value

            val watermarkData = WatermarkData(
                latitude = state.currentLocation?.latitude,
                longitude = state.currentLocation?.longitude,
                altitudeM = state.currentLocation?.altitudeM,
                accuracyM = state.currentLocation?.accuracyM,
                utmCoordinate = state.currentUtm?.toFormattedString(),
                azimuthDeg = state.currentCompass?.azimuthDeg,
                timestampEpochMs = System.currentTimeMillis(),
                mapProjectName = state.projectName.ifBlank { null },
                note = state.quickNote.trim().ifBlank { null },
                config = state.watermarkConfig
            )

            val photosDir = File(context.filesDir, "photos/$projectId")
            photosDir.mkdirs()
            val photoId = UUID.randomUUID().toString()
            val photoFile = File(photosDir, "$photoId.jpg")

            val saveResult = photoProcessor.processAndSavePhoto(capturedImage, watermarkData, photoFile)
            if (saveResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isSavingPhoto = false,
                        errorMessage = saveResult.exceptionOrNull()?.message ?: "Gagal memproses watermark foto"
                    )
                }
                return@launch
            }

            // Save to database
            val waypointId = UUID.randomUUID().toString()
            val wpCount = waypointDao.getWaypointCountForProject(projectId)
            val title = "FOTO-${String.format(Locale.US, "%03d", wpCount + 1)}"

            val waypoint = WaypointEntity(
                id = waypointId,
                projectId = projectId,
                title = title,
                note = state.quickNote.trim(),
                latitude = state.currentLocation?.latitude ?: 0.0,
                longitude = state.currentLocation?.longitude ?: 0.0,
                altitudeM = state.currentLocation?.altitudeM,
                accuracyM = state.currentLocation?.accuracyM,
                hasPhoto = true,
                createdAtEpochMs = System.currentTimeMillis()
            )
            waypointDao.insert(waypoint)

            val photoRecord = PhotoRecordEntity(
                id = photoId,
                waypointId = waypointId,
                filePath = photoFile.absolutePath,
                azimuthDeg = state.currentCompass?.azimuthDeg,
                capturedAtEpochMs = System.currentTimeMillis()
            )
            photoRecordDao.insert(photoRecord)

            // Persist last captured photo path
            stampSettingsManager.setLastPhotoPath(photoFile.absolutePath)

            _uiState.update {
                it.copy(
                    isSavingPhoto = false,
                    photoSavedWaypointId = waypointId,
                    lastCapturedPhotoPath = photoFile.absolutePath,
                    userNotice = "Foto berhasil disimpan dan ditautkan ke peta."
                )
            }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null, errorMessage = null) }
    }
}
