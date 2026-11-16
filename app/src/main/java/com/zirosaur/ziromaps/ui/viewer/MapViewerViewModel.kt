package com.zirosaur.ziromaps.ui.viewer

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.geopdf.tile.MapTile
import com.zirosaur.ziromaps.core.geopdf.tile.PdfTileRenderer
import com.zirosaur.ziromaps.core.location.converter.UtmConverter
import com.zirosaur.ziromaps.core.location.model.CompassData
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.provider.CompassProvider
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
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
import javax.inject.Inject

@HiltViewModel
class MapViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mapProjectDao: MapProjectDao,
    private val locationProvider: LocationProvider,
    private val compassProvider: CompassProvider,
    val tileRenderer: PdfTileRenderer
) : ViewModel() {

    private val projectId: String = savedStateHandle.get<String>("projectId").orEmpty()

    private val _uiState = MutableStateFlow(MapViewerUiState())
    val uiState: StateFlow<MapViewerUiState> = _uiState.asStateFlow()

    private var locationJob: Job? = null

    init {
        if (projectId.isNotBlank()) {
            observeProject(projectId)
        }
        observeCompass()
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

    fun dismissUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
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
