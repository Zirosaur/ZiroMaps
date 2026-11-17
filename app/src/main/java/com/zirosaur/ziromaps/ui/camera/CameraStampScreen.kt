package com.zirosaur.ziromaps.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.core.camera.model.FlashMode
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkPosition
import com.zirosaur.ziromaps.ui.camera.components.LiveWatermarkOverlay
import com.zirosaur.ziromaps.ui.camera.components.StampSettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraStampScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onPhotoSaved: (waypointId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraStampViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Asynchronously load thumbnail of last photo
    val lastPhotoBitmap by produceState<Bitmap?>(initialValue = null, key1 = uiState.lastCapturedPhotoPath) {
        val path = uiState.lastCapturedPhotoPath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                value = withContext(Dispatchers.IO) {
                    try {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    LaunchedEffect(projectId) {
        if (projectId.isNotBlank()) {
            viewModel.setProjectId(projectId)
        }
    }

    // Permission launcher for CAMERA
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    // Check camera permission on resume
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Unbind camera on dispose
    DisposableEffect(lifecycleOwner) {
        onDispose {
            viewModel.unbindCamera()
        }
    }

    // Handle photo saved event
    LaunchedEffect(uiState.photoSavedWaypointId) {
        uiState.photoSavedWaypointId?.let { waypointId ->
            onPhotoSaved(waypointId)
        }
    }

    // Handle notices / errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (uiState.cameraPermissionGranted) {
                // 1. Camera Preview Viewfinder
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                            viewModel.bindCamera(lifecycleOwner, this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                previewViewRef?.let { pv ->
                                    viewModel.focusAndMeter(offset.x, offset.y, pv)
                                }
                            }
                        }
                )

                // 2. Top Controls Bar: Back, GPS Status, Flash, Settings, Flip Camera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Peta",
                                tint = Color.White
                            )
                        }
                    }

                    // GPS Status Badge
                    val (badgeText, badgeColor) = when {
                        !uiState.isGpsFixed -> Pair("Mencari GPS…", Color(0xFFFFB300))
                        !uiState.isHighAccuracy -> Pair(
                            "±${uiState.currentLocation?.accuracyM?.toInt()}m (Rendah)",
                            Color(0xFFFF9800)
                        )
                        else -> Pair(
                            "GPS Fix ±${uiState.currentLocation?.accuracyM?.toInt()}m",
                            Color(0xFF4CAF50)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color = badgeColor, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Actions: Flash, Stamp Settings, Flip Camera
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Flash Toggle
                        val flashIconRes = when (uiState.flashMode) {
                            FlashMode.ON -> R.drawable.ic_flash_on
                            FlashMode.AUTO -> R.drawable.ic_flash_auto
                            FlashMode.OFF -> R.drawable.ic_flash_off
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = { viewModel.toggleFlashMode() }) {
                                Icon(
                                    painter = painterResource(id = flashIconRes),
                                    contentDescription = "Flash Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Stamp Settings
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = { viewModel.openStampSettings() }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pengaturan Stamp",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Flip Camera
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = {
                                previewViewRef?.let { pv ->
                                    viewModel.toggleLensFacing(lifecycleOwner, pv)
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flip_camera),
                                    contentDescription = "Ganti Kamera",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Live WYSIWYG Watermark Preview (Positioned according to config)
                val watermarkAlignment = when (uiState.watermarkConfig.position) {
                    WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                    WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                }
                val watermarkPaddingModifier = when (uiState.watermarkConfig.position) {
                    WatermarkPosition.BOTTOM_LEFT -> Modifier
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 150.dp)
                    WatermarkPosition.BOTTOM_RIGHT -> Modifier
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 150.dp)
                    WatermarkPosition.TOP_LEFT -> Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 76.dp)
                }

                LiveWatermarkOverlay(
                    uiState = uiState,
                    modifier = Modifier
                        .align(watermarkAlignment)
                        .then(watermarkPaddingModifier)
                )

                // 4. Quick Zoom Selector Chips (e.g. 0.6x, 1x, 2x)
                val zoomRatios = remember(uiState.minZoomRatio, uiState.maxZoomRatio) {
                    val list = mutableListOf<Float>()
                    if (uiState.minZoomRatio <= 0.65f) list.add(0.6f)
                    list.add(1.0f)
                    if (uiState.maxZoomRatio >= 2.0f) list.add(2.0f)
                    if (uiState.maxZoomRatio >= 5.0f) list.add(5.0f)
                    list
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    zoomRatios.forEach { ratio ->
                        val isSelected = kotlin.math.abs(uiState.zoomRatio - ratio) < 0.15f
                        val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f)
                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(bg)
                                .clickable { viewModel.setZoomRatio(ratio) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ratio % 1.0f == 0f) "${ratio.toInt()}x" else "${ratio}x",
                                color = textCol,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 5. Ergonomic Bottom Shutter Dock
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Last Photo Thumbnail
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.DarkGray)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = lastPhotoBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Foto Terakhir",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_camera),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Center: Prominent Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .border(3.5.dp, Color.White, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isSavingPhoto) Color.Gray else Color.White)
                                .clickable(enabled = !uiState.isSavingPhoto) {
                                    viewModel.takePhoto(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSavingPhoto) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        // Right: Quick Note Pill Trigger (shows note if exists)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { viewModel.openStampSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Buka Pengaturan Stamp",
                                tint = if (uiState.quickNote.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 6. Stamp Settings Bottom Sheet
                if (uiState.showStampSettings) {
                    StampSettingsSheet(
                        config = uiState.watermarkConfig,
                        quickNote = uiState.quickNote,
                        onQuickNoteChange = { viewModel.updateQuickNote(it) },
                        onConfigChange = { viewModel.updateWatermarkConfig(it) },
                        onDismiss = { viewModel.dismissStampSettings() }
                    )
                }
            } else {
                // Permission Denied View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Izin Kamera Dibutuhkan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.camera_permission_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text(stringResource(id = R.string.grant_camera_permission))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(id = R.string.open_settings))
                    }
                }
            }
        }
    }
}
