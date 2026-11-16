package com.zirosaur.ziromaps.core.camera.orientation

import android.content.Context
import android.view.OrientationEventListener
import com.zirosaur.ziromaps.core.camera.model.DeviceOrientation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DeviceOrientationListener @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _orientation = MutableStateFlow(DeviceOrientation.PORTRAIT_UP)
    val orientation: StateFlow<DeviceOrientation> = _orientation.asStateFlow()

    private val _rawAngle = MutableStateFlow(0)
    val rawAngle: StateFlow<Int> = _rawAngle.asStateFlow()

    private var orientationEventListener: OrientationEventListener? = null

    open fun start() {
        if (orientationEventListener == null) {
            orientationEventListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientationAngle: Int) {
                    if (orientationAngle == ORIENTATION_UNKNOWN) return
                    _rawAngle.value = orientationAngle
                    val newOrientation = DeviceOrientation.fromDegrees(orientationAngle)
                    if (newOrientation != _orientation.value) {
                        _orientation.value = newOrientation
                    }
                }
            }
        }
        if (orientationEventListener?.canDetectOrientation() == true) {
            orientationEventListener?.enable()
        }
    }

    open fun stop() {
        orientationEventListener?.disable()
    }

    fun setOrientationForTesting(orientation: DeviceOrientation) {
        _orientation.value = orientation
        _rawAngle.value = orientation.degrees
    }
}
