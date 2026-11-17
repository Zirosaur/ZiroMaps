package com.zirosaur.ziromaps.core.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.zirosaur.ziromaps.core.camera.model.CameraState
import com.zirosaur.ziromaps.core.camera.model.CapturedImage
import com.zirosaur.ziromaps.core.camera.model.DeviceOrientation
import com.zirosaur.ziromaps.core.camera.model.FlashMode
import com.zirosaur.ziromaps.core.camera.model.LensFacing
import com.zirosaur.ziromaps.core.camera.orientation.DeviceOrientationListener
import com.zirosaur.ziromaps.core.camera.util.ImageRotationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CameraXManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val orientationListener: DeviceOrientationListener
) : CameraXManager {

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Uninitialized)
    override val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    override val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    private val _lensFacing = MutableStateFlow(LensFacing.BACK)
    override val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1f)
    override val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _minZoomRatio = MutableStateFlow(1f)
    override val minZoomRatio: StateFlow<Float> = _minZoomRatio.asStateFlow()

    private val _maxZoomRatio = MutableStateFlow(8f)
    override val maxZoomRatio: StateFlow<Float> = _maxZoomRatio.asStateFlow()

    override val orientation: StateFlow<DeviceOrientation> = orientationListener.orientation

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var orientationJob: Job? = null

    override fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        initialLensFacing: LensFacing
    ) {
        _lensFacing.value = initialLensFacing
        _cameraState.value = CameraState.Starting

        orientationListener.start()

        orientationJob?.cancel()
        orientationJob = lifecycleOwner.lifecycleScope.launch {
            orientationListener.orientation.collect { deviceOrientation ->
                imageCapture?.targetRotation = deviceOrientation.surfaceRotation
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                setupAndBindCamera(lifecycleOwner, previewView, provider)
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error("Gagal menginisialisasi kamera: ${e.message}", e)
            }
        }, mainExecutor)
    }

    private fun setupAndBindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        provider: ProcessCameraProvider
    ) {
        val selector = if (_lensFacing.value == LensFacing.FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val currentRotation = orientationListener.orientation.value.surfaceRotation

        val newPreview = Preview.Builder()
            .setTargetRotation(currentRotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val newImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(_flashMode.value.toCameraXFlashMode())
            .setTargetRotation(currentRotation)
            .build()

        this.preview = newPreview
        this.imageCapture = newImageCapture

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                newPreview,
                newImageCapture
            )
            this.camera = boundCamera
            boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zs ->
                if (zs != null) {
                    _zoomRatio.value = zs.zoomRatio
                    _minZoomRatio.value = zs.minZoomRatio
                    _maxZoomRatio.value = zs.maxZoomRatio
                }
            }
            _cameraState.value = CameraState.Ready
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error("Gagal mengaitkan lifecycle kamera: ${e.message}", e)
        }
    }

    override fun unbind() {
        orientationJob?.cancel()
        orientationJob = null
        orientationListener.stop()

        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        _cameraState.value = CameraState.Uninitialized
    }

    override fun setFlashMode(mode: FlashMode) {
        _flashMode.value = mode
        imageCapture?.flashMode = mode.toCameraXFlashMode()
    }

    override fun toggleFlashMode() {
        val nextMode = when (_flashMode.value) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        setFlashMode(nextMode)
    }

    override fun setLensFacing(
        facing: LensFacing,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        if (_lensFacing.value == facing) return
        _lensFacing.value = facing
        val provider = cameraProvider ?: return
        setupAndBindCamera(lifecycleOwner, previewView, provider)
    }

    override fun toggleLensFacing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val nextFacing = if (_lensFacing.value == LensFacing.BACK) LensFacing.FRONT else LensFacing.BACK
        setLensFacing(nextFacing, lifecycleOwner, previewView)
    }

    override fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
        _zoomRatio.value = ratio
    }

    override fun setLinearZoom(linearZoom: Float) {
        camera?.cameraControl?.setLinearZoom(linearZoom)
    }

    override fun focusAndMeter(x: Float, y: Float, previewView: PreviewView) {
        val cam = camera ?: return
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (_: Exception) {
        }
    }

    override suspend fun capturePhoto(): Result<CapturedImage> = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null || _cameraState.value !is CameraState.Ready) {
            continuation.resume(Result.failure(IllegalStateException("Kamera belum siap")))
            return@suspendCancellableCoroutine
        }

        _cameraState.value = CameraState.Capturing

        // Refresh latest target rotation right before capture
        val currentOrientation = orientationListener.orientation.value
        capture.targetRotation = currentOrientation.surfaceRotation

        val executor = ContextCompat.getMainExecutor(context)
        capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rawBitmap = imageProxy.toBitmap()
                    imageProxy.close()

                    val isFront = _lensFacing.value == LensFacing.FRONT
                    val rotatedBitmap = ImageRotationHelper.rotateBitmap(
                        source = rawBitmap,
                        degrees = rotationDegrees,
                        recycleSource = true
                    )
                    val finalBitmap = if (isFront) {
                        ImageRotationHelper.mirrorBitmap(rotatedBitmap, recycleSource = true)
                    } else {
                        rotatedBitmap
                    }

                    val capturedImage = CapturedImage(
                        bitmap = finalBitmap,
                        rotationDegrees = rotationDegrees,
                        width = finalBitmap.width,
                        height = finalBitmap.height,
                        timestampEpochMs = System.currentTimeMillis(),
                        isMirrored = isFront
                    )

                    _cameraState.value = CameraState.Ready
                    continuation.resume(Result.success(capturedImage))
                } catch (e: Throwable) {
                    _cameraState.value = CameraState.Ready
                    continuation.resume(Result.failure(e))
                }
            }

            override fun onError(exception: ImageCaptureException) {
                _cameraState.value = CameraState.Error("Gagal mengambil foto: ${exception.message}", exception)
                continuation.resume(Result.failure(exception))
            }
        })
    }
}
