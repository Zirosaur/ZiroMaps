package com.zirosaur.ziromaps.core.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zirosaur.ziromaps.core.camera.model.CameraState
import com.zirosaur.ziromaps.core.camera.model.CapturedImage
import com.zirosaur.ziromaps.core.camera.model.DeviceOrientation
import com.zirosaur.ziromaps.core.camera.model.FlashMode
import com.zirosaur.ziromaps.core.camera.model.LensFacing
import kotlinx.coroutines.flow.StateFlow

interface CameraXManager {
    val cameraState: StateFlow<CameraState>
    val flashMode: StateFlow<FlashMode>
    val lensFacing: StateFlow<LensFacing>
    val zoomRatio: StateFlow<Float>
    val minZoomRatio: StateFlow<Float>
    val maxZoomRatio: StateFlow<Float>
    val orientation: StateFlow<DeviceOrientation>

    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        initialLensFacing: LensFacing = LensFacing.BACK
    )

    fun unbind()

    fun setFlashMode(mode: FlashMode)
    fun toggleFlashMode()
    fun setLensFacing(facing: LensFacing, lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun toggleLensFacing(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun setZoomRatio(ratio: Float)
    fun setLinearZoom(linearZoom: Float)
    fun focusAndMeter(x: Float, y: Float, previewView: PreviewView)

    suspend fun capturePhoto(): Result<CapturedImage>
}
