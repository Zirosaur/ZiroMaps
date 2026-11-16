package com.zirosaur.ziromaps.ui.viewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.dao.WaypointDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.entity.WaypointEntity
import com.zirosaur.ziromaps.core.database.model.WaypointWithPhotos
import com.zirosaur.ziromaps.core.export.ExportManager
import com.zirosaur.ziromaps.core.export.model.ExportFormat
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.geopdf.tile.MapTile
import com.zirosaur.ziromaps.core.geopdf.tile.PdfTileRenderer
import com.zirosaur.ziromaps.core.location.converter.UtmConverter
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
import com.zirosaur.ziromaps.service.TrackRecordingManager
import com.zirosaur.ziromaps.service.TrackRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MapViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mapProjectDao: MapProjectDao,
    private val waypointDao: WaypointDao,
    private val trackDao: TrackDao,
    private val locationProvider: LocationProvider,
    private val compassProvider: CompassProvider,
    private val trackRecordingManager: TrackRecordingManager,
    private val exportManager: ExportManager,
    val tileRenderer: PdfTileRenderer
) : ViewModel() {

    private val projectId: String = savedStateHandle.get<String>("projectId").orEmpty()

    private val _uiState = MutableStateFlow(MapViewerUiState())
    val uiState: StateFlow<MapViewerUiState> = _uiState.asStateFlow()

    private var locationJob: Job? = null
    private var waypointsJob: Job? = null
    private var tracksJob: Job? = null

    init {
        if (projectId.isNotBlank()) {
            observeProject(projectId)
        }
        observeCompass()
        observeRecordingManager()
        startLocationUpdates()
    }

    fun setProjectId(id: String) {
        if (id.isNotBlank() && id != _uiState.value.mapProject?.id) {
            observeProject(id)
        }
    }

    private fun observeProject(id: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        mapProjectDao.getProjectById(id)
            .onEach { project ->
                if (project != null) {
                    val matrix = try {
                        TransformMatrix.fromJson(project.transformMatrixJson)
                    } catch (_: Exception) {
                        null
                    }
                    _uiState.update { current ->
                        val loc = current.currentLocation
                        val pagePt = if (loc != null && matrix != null) {
                            matrix.toPagePoint(loc.latitude, loc.longitude)
                        } else null
                        val inside = if (loc != null) {
                            checkIfInside(loc.latitude, loc.longitude, pagePt, matrix, project)
                        } else false

                        current.copy(
                            isLoading = false,
                            mapProject = project,
                            transformMatrix = matrix,
                            userPagePoint = pagePt,
                            isGpsInsideBounds = inside
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Peta tidak ditemukan di database."
                        )
                    }
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat peta: ${throwable.localizedMessage}"
                    )
                }
            }
            .launchIn(viewModelScope)

        observeWaypoints(id)
        observeTracks(id)
    }

    private fun observeWaypoints(projectId: String) {
        waypointsJob?.cancel()
        waypointsJob = waypointDao.getWaypointsWithPhotosForProject(projectId)
            .onEach { list ->
                _uiState.update { current ->
                    // Also update selected waypoint if modified
                    val updatedSelected = current.selectedWaypoint?.let { sel ->
                        list.find { it.waypoint.id == sel.waypoint.id }
                    }
                    current.copy(waypoints = list, selectedWaypoint = updatedSelected)
                }
            }
            .catch { /* Fallback silent */ }
            .launchIn(viewModelScope)
    }

    private fun observeTracks(projectId: String) {
        tracksJob?.cancel()
        tracksJob = trackDao.getTracksWithPointsForProject(projectId)
            .onEach { list ->
                _uiState.update { it.copy(tracks = list) }
            }
            .catch { /* Fallback silent */ }
            .launchIn(viewModelScope)
    }

    private fun observeRecordingManager() {
        trackRecordingManager.isRecording
            .onEach { recording ->
                _uiState.update { it.copy(isRecordingTrack = recording) }
            }
            .launchIn(viewModelScope)

        trackRecordingManager.recordedDistanceM
            .onEach { distance ->
                _uiState.update { it.copy(recordedDistanceM = distance) }
            }
            .launchIn(viewModelScope)
    }

    fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                locationProvider.getLocationUpdates(intervalMs = 1000L)
                    .collect { location ->
                        _uiState.update { current ->
                            val matrix = current.transformMatrix
                            val project = current.mapProject
                            val pagePt = matrix?.toPagePoint(location.latitude, location.longitude)
                            val utm = UtmConverter.latLonToUtm(location.latitude, location.longitude)
                            val inside = checkIfInside(location.latitude, location.longitude, pagePt, matrix, project)

                            current.copy(
                                currentLocation = location,
                                currentUtm = utm,
                                userPagePoint = pagePt,
                                isGpsInsideBounds = inside,
                                hasLocationPermission = true
                            )
                        }
                    }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(hasLocationPermission = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Sensor lokasi bermasalah: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun observeCompass() {
        compassProvider.getCompassUpdates()
            .onEach { compass ->
                _uiState.update { it.copy(currentCompass = compass) }
            }
            .catch { /* Compass fallback silent */ }
            .launchIn(viewModelScope)
    }

    fun toggleCoordinateFormat() {
        _uiState.update { it.copy(coordinateFormat = it.coordinateFormat.next()) }
    }

    fun centerOnGps() {
        val loc = _uiState.value.currentLocation
        if (loc == null) {
            _uiState.update { it.copy(userNotice = "Menunggu sinyal GPS fix…") }
            return
        }
        if (!_uiState.value.isGpsInsideBounds) {
            _uiState.update { it.copy(userNotice = "Posisi GPS berada di luar area peta ini.") }
            return
        }
        _uiState.update { it.copy(centerGpsTrigger = System.currentTimeMillis()) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            startLocationUpdates()
        }
    }

    fun updateMapCenter(center: LatLon?) {
        _uiState.update { it.copy(mapCenterLatLon = center) }
    }

    fun openAddWaypointDialog() {
        _uiState.update { it.copy(showAddWaypointDialog = true) }
    }

    fun dismissAddWaypointDialog() {
        _uiState.update { it.copy(showAddWaypointDialog = false) }
    }

    fun saveWaypoint(
        title: String,
        note: String,
        latitude: Double,
        longitude: Double,
        altitudeM: Double?,
        accuracyM: Float?
    ) {
        val projId = _uiState.value.mapProject?.id ?: return
        viewModelScope.launch {
            val waypoint = WaypointEntity(
                id = UUID.randomUUID().toString(),
                projectId = projId,
                title = title,
                note = note,
                latitude = latitude,
                longitude = longitude,
                altitudeM = altitudeM,
                accuracyM = accuracyM,
                hasPhoto = false,
                createdAtEpochMs = System.currentTimeMillis()
            )
            waypointDao.insert(waypoint)
            _uiState.update {
                it.copy(
                    showAddWaypointDialog = false,
                    userNotice = "Waypoint \"$title\" berhasil disimpan."
                )
            }
        }
    }

    fun selectWaypoint(wp: WaypointWithPhotos) {
        _uiState.update { it.copy(selectedWaypoint = wp) }
    }

    fun dismissWaypointDetail() {
        _uiState.update { it.copy(selectedWaypoint = null) }
    }

    fun updateWaypoint(waypointId: String, newTitle: String, newNote: String) {
        viewModelScope.launch {
            val existing = waypointDao.getWaypointByIdSync(waypointId)
            if (existing != null) {
                waypointDao.update(existing.copy(title = newTitle, note = newNote))
                _uiState.update { it.copy(userNotice = "Waypoint berhasil diperbarui.") }
            }
        }
    }

    fun deleteWaypoint(waypointId: String) {
        viewModelScope.launch {
            waypointDao.deleteById(waypointId)
            _uiState.update {
                it.copy(
                    selectedWaypoint = null,
                    userNotice = "Waypoint telah dihapus."
                )
            }
        }
    }

    fun toggleTrackRecording(context: Context) {
        val project = _uiState.value.mapProject ?: return
        if (_uiState.value.isRecordingTrack) {
            TrackRecordingService.stop(context)
            _uiState.update { it.copy(userNotice = "Perekaman track dihentikan.") }
        } else {
            val trackName = "Track ${System.currentTimeMillis() % 10000}"
            TrackRecordingService.start(context, project.id, trackName)
            _uiState.update { it.copy(userNotice = "Perekaman track dimulai.") }
        }
    }

    fun dismissUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun openExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportDialog() {
        if (!_uiState.value.isExporting) {
            _uiState.update { it.copy(showExportDialog = false) }
        }
    }

    fun exportProject(
        format: ExportFormat,
        context: Context,
        onFileReadyToShare: ((File, ExportFormat) -> Unit)? = null
    ) {
        val project = _uiState.value.mapProject ?: return
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val destinationDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val result = exportManager.exportProject(project.id, format, destinationDir)
            result.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            showExportDialog = false,
                            userNotice = "File ${file.name} siap dibagikan."
                        )
                    }
                    if (onFileReadyToShare != null) {
                        onFileReadyToShare(file, format)
                    } else {
                        shareExportedFile(context, file, format)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            userNotice = "Gagal mengekspor data: ${error.localizedMessage ?: "Terjadi kesalahan"}"
                        )
                    }
                }
            )
        }
    }

    fun shareExportedFile(context: Context, file: File, format: ExportFormat) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Data Peta: ${file.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Berikut file ekspor data proyek peta \"${_uiState.value.mapProject?.name ?: file.name}\" dari Ziro Maps."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserTitle = "Bagikan Proyek Peta (${format.displayName})"
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(userNotice = "File ${file.name} siap, namun gagal membuka Share Sheet: ${e.localizedMessage}")
            }
        }
    }

    suspend fun renderTile(tile: MapTile): Bitmap? {
        val project = _uiState.value.mapProject ?: return null
        val pdfFile = File(project.pdfLocalPath)
        if (!pdfFile.exists()) return null
        return tileRenderer.renderTile(pdfFile, tile, pageIndex = 0)
    }

    private fun checkIfInside(
        lat: Double,
        lon: Double,
        pagePt: PagePoint?,
        matrix: TransformMatrix?,
        project: MapProjectEntity?
    ): Boolean {
        if (matrix != null && matrix.boundsLatLon.size >= 3) {
            return isInsidePolygon(lat, lon, matrix.boundsLatLon)
        }
        if (pagePt != null && project != null) {
            return pagePt.xPt in 0.0..project.pageWidthPt &&
                    pagePt.yPt in 0.0..project.pageHeightPt
        }
        return false
    }

    private fun isInsidePolygon(lat: Double, lon: Double, polygon: List<LatLon>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.longitude > lon) != (pj.longitude > lon) &&
                lat < (pj.latitude - pi.latitude) * (lon - pi.longitude) / (pj.longitude - pi.longitude) + pi.latitude
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
