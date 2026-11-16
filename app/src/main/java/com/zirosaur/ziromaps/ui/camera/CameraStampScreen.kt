package com.zirosaur.ziromaps.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.zirosaur.ziromaps.ui.camera.components.LiveWatermarkOverlay

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
                // Camera Preview View
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

                // Top Controls Overlay
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
                            "Akurasi: ±${uiState.currentLocation?.accuracyM?.toInt()}m (Rendah)",
                            Color(0xFFFF9800)
                        )
                        else -> Pair(
                            "GPS Fix (±${uiState.currentLocation?.accuracyM?.toInt()}m)",
                            Color(0xFF4CAF50)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
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

                    // Action buttons (Flash & Flip)
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
                                    modifier = Modifier.size(22.dp)
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Live Watermark Preview (Bottom Left above controls)
                LiveWatermarkOverlay(
                    uiState = uiState,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 156.dp)
                )

                // Bottom Controls: Quick Note & Shutter Button
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Note Input
                    OutlinedTextField(
                        value = uiState.quickNote,
                        onValueChange = { viewModel.updateQuickNote(it) },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.quick_note_placeholder),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .border(3.dp, Color.White, CircleShape)
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
