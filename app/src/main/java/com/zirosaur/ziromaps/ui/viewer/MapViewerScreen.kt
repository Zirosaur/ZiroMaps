package com.zirosaur.ziromaps.ui.viewer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.ui.viewer.components.CoordinateStatusBar
import com.zirosaur.ziromaps.ui.viewer.components.MapCanvas
import com.zirosaur.ziromaps.ui.viewer.components.MapViewerBottomBar
import kotlinx.coroutines.launch

@Composable
fun MapViewerScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val project = uiState.mapProject

            if (project != null) {
                // Interactive Tiled Map Canvas
                MapCanvas(
                    mapId = project.id,
                    pageWidthPt = project.pageWidthPt,
                    pageHeightPt = project.pageHeightPt,
                    userPagePoint = uiState.userPagePoint,
                    userAccuracyM = uiState.currentLocation?.accuracyM,
                    userAzimuthDeg = uiState.currentCompass?.azimuthDeg,
                    centerGpsTrigger = uiState.centerGpsTrigger,
                    loadTileBitmap = { tile -> viewModel.renderTile(tile) },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Bar Overlay: Back button + Live Coordinate Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 12.dp, end = 12.dp),
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

                    Spacer(modifier = Modifier.width(10.dp))

                    CoordinateStatusBar(
                        location = uiState.currentLocation,
                        utm = uiState.currentUtm,
                        compass = uiState.currentCompass,
                        format = uiState.coordinateFormat,
                        isInsideBounds = uiState.isGpsInsideBounds,
                        onToggleFormat = { viewModel.toggleCoordinateFormat() },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom Action Buttons Overlay
                MapViewerBottomBar(
                    onCenterGpsClick = { viewModel.centerOnGps() },
                    onAddPinClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Tambah Waypoint (Tersedia di T2.4)")
                        }
                    },
                    onRecordTrackClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Perekaman Track (Tersedia di T2.4)")
                        }
                    },
                    onCameraStampClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Kamera Stamp (Tersedia di Phase 3)")
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else if (uiState.isLoading) {
                // Loading state
                Column(
                    modifier = Modifier.align(Alignment.Center),
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
                    modifier = Modifier.align(Alignment.Center),
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
