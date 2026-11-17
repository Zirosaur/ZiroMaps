package com.zirosaur.ziromaps.ui.viewer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.ui.viewer.components.AddWaypointDialog
import com.zirosaur.ziromaps.ui.viewer.components.CoordinateStatusBar
import com.zirosaur.ziromaps.ui.viewer.components.ExportDialog
import com.zirosaur.ziromaps.ui.viewer.components.FloatingCompass
import com.zirosaur.ziromaps.ui.viewer.components.LayersBottomSheet
import com.zirosaur.ziromaps.ui.viewer.components.MapCanvas
import com.zirosaur.ziromaps.ui.viewer.components.MapCrosshair
import com.zirosaur.ziromaps.ui.viewer.components.MapScaleBar
import com.zirosaur.ziromaps.ui.viewer.components.MapViewerBottomBar
import com.zirosaur.ziromaps.ui.viewer.components.WaypointDetailSheet
import kotlinx.coroutines.delay

@Composable
fun MapViewerScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onOpenCameraStamp: (projectId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MapViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val liveTrackDurationSec by produceState(initialValue = 0L, key1 = uiState.isRecordingTrack) {
        if (uiState.isRecordingTrack) {
            val startMs = uiState.trackRecordingStartTime ?: System.currentTimeMillis()
            while (true) {
                value = ((System.currentTimeMillis() - startMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            value = 0L
        }
    }

    LaunchedEffect(projectId) {
        if (projectId.isNotBlank()) {
            viewModel.setProjectId(projectId)
        }
    }

    // Location permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val granted = fineGranted || coarseGranted
        viewModel.onLocationPermissionResult(granted)
    }

    // Check location permission on start
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val granted = fineGranted || coarseGranted
        viewModel.onLocationPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Snackbar event notifications
    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissUserNotice()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val project = uiState.mapProject

            if (project != null) {
                // 1. Full-Bleed Interactive Tiled Map Canvas
                MapCanvas(
                    mapId = project.id,
                    pageWidthPt = project.pageWidthPt,
                    pageHeightPt = project.pageHeightPt,
                    transformMatrix = uiState.transformMatrix,
                    userPagePoint = uiState.userPagePoint,
                    userAccuracyM = uiState.currentLocation?.accuracyM,
                    userAzimuthDeg = uiState.currentCompass?.azimuthDeg,
                    followMode = uiState.followMode,
                    rotationDeg = uiState.mapRotationDeg,
                    panToLatLon = uiState.panToLatLon,
                    centerGpsTrigger = uiState.centerGpsTrigger,
                    waypoints = uiState.waypoints,
                    tracks = uiState.tracks,
                    onWaypointClick = { waypoint -> viewModel.selectWaypoint(waypoint) },
                    onMapCenterChanged = { centerLatLon -> viewModel.updateMapCenter(centerLatLon) },
                    onMetersPerPixelChanged = { mpp -> viewModel.updateMetersPerPixel(mpp) },
                    onUserPan = { viewModel.onUserPan() },
                    loadTileBitmap = { tile -> viewModel.renderTile(tile) },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. High-Contrast Center Targeting Crosshair
                MapCrosshair(modifier = Modifier.fillMaxSize())

                // 3. Top Floating Overlays
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    // Top Bar: Back button + Live Coordinate Status Bar + Export button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Perpustakaan",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        CoordinateStatusBar(
                            location = uiState.currentLocation,
                            utm = uiState.currentUtm,
                            crosshairLatLon = uiState.mapCenterLatLon,
                            crosshairUtm = uiState.crosshairUtm,
                            compass = uiState.currentCompass,
                            format = uiState.coordinateFormat,
                            isInsideBounds = uiState.isGpsInsideBounds,
                            onToggleFormat = { viewModel.toggleCoordinateFormat() },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.openExportDialog() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share),
                                    contentDescription = stringResource(id = R.string.export_button_desc),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary row: Floating Compass (tap to reset North) & Dynamic Scale Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingCompass(
                            rotationDeg = uiState.mapRotationDeg,
                            onResetNorth = { viewModel.resetNorth() }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        MapScaleBar(metersPerPixel = uiState.metersPerPixel)
                    }
                }

                // 4. Ergonomic Bottom Action Bar
                MapViewerBottomBar(
                    followMode = uiState.followMode,
                    isRecordingTrack = uiState.isRecordingTrack,
                    recordedDistanceM = uiState.recordedDistanceM,
                    recordingDurationSec = liveTrackDurationSec,
                    onCycleFollowMode = { viewModel.cycleFollowMode() },
                    onAddPinClick = { viewModel.openAddWaypointDialog() },
                    onRecordTrackClick = { viewModel.toggleTrackRecording(context) },
                    onLayersClick = { viewModel.openLayersSheet() },
                    onCameraStampClick = { onOpenCameraStamp(projectId) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )

                // 5. Layers Bottom Sheet (Pins & Tracks list)
                if (uiState.showLayersSheet) {
                    LayersBottomSheet(
                        waypoints = uiState.waypoints,
                        tracks = uiState.tracks,
                        onSelectWaypoint = { wp ->
                            viewModel.panToPoint(com.zirosaur.ziromaps.core.geopdf.model.LatLon(wp.waypoint.latitude, wp.waypoint.longitude))
                            viewModel.selectWaypoint(wp)
                        },
                        onDeleteWaypoint = { wpId -> viewModel.deleteWaypoint(wpId) },
                        onSelectTrack = { trk ->
                            val firstPt = trk.points.firstOrNull()
                            if (firstPt != null) {
                                viewModel.panToPoint(com.zirosaur.ziromaps.core.geopdf.model.LatLon(firstPt.latitude, firstPt.longitude))
                            }
                        },
                        onDeleteTrack = { trackId -> viewModel.deleteTrack(trackId) },
                        onDismiss = { viewModel.dismissLayersSheet() }
                    )
                }

                // 6. Add Waypoint Dialog (supports crosshair or GPS)
                if (uiState.showAddWaypointDialog) {
                    val defaultTitle = "WP-${String.format(java.util.Locale.US, "%03d", uiState.waypoints.size + 1)}"
                    AddWaypointDialog(
                        defaultTitle = defaultTitle,
                        gpsLocation = uiState.currentLocation,
                        mapCenterLatLon = uiState.mapCenterLatLon,
                        onDismiss = { viewModel.dismissAddWaypointDialog() },
                        onSave = { title, note, lat, lon, alt, acc ->
                            viewModel.saveWaypoint(title, note, lat, lon, alt, acc)
                        }
                    )
                }

                // 7. Waypoint Detail Bottom Sheet
                uiState.selectedWaypoint?.let { selected ->
                    WaypointDetailSheet(
                        waypointWithPhotos = selected,
                        onDismiss = { viewModel.dismissWaypointDetail() },
                        onEdit = { wpId, newTitle, newNote ->
                            viewModel.updateWaypoint(wpId, newTitle, newNote)
                        },
                        onDelete = { wpId ->
                            viewModel.deleteWaypoint(wpId)
                        }
                    )
                }

                // 8. Export Dialog
                if (uiState.showExportDialog) {
                    ExportDialog(
                        projectName = project.name,
                        waypointCount = uiState.waypoints.size,
                        trackCount = uiState.tracks.size,
                        isExporting = uiState.isExporting,
                        onDismiss = { viewModel.dismissExportDialog() },
                        onExport = { format -> viewModel.exportProject(format, context) }
                    )
                }
            } else if (uiState.isLoading) {
                // Loading state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Memuat peta GeoPDF…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Error state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Gagal memuat peta.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
