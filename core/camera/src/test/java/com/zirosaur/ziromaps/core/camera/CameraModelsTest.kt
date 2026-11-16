package com.zirosaur.ziromaps.core.camera

import androidx.camera.core.ImageCapture
import com.zirosaur.ziromaps.core.camera.model.CameraState
import com.zirosaur.ziromaps.core.camera.model.FlashMode
import com.zirosaur.ziromaps.core.camera.model.LensFacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraModelsTest {

    @Test
    fun flashMode_mappingToCameraXConstants() {
        assertEquals(ImageCapture.FLASH_MODE_OFF, FlashMode.OFF.toCameraXFlashMode())
        assertEquals(ImageCapture.FLASH_MODE_ON, FlashMode.ON.toCameraXFlashMode())
        assertEquals(ImageCapture.FLASH_MODE_AUTO, FlashMode.AUTO.toCameraXFlashMode())
    }

    @Test
    fun lensFacing_values() {
        assertEquals(2, LensFacing.entries.size)
        assertTrue(LensFacing.entries.contains(LensFacing.BACK))
        assertTrue(LensFacing.entries.contains(LensFacing.FRONT))
    }

    @Test
    fun cameraState_types() {
        val states: List<CameraState> = listOf(
            CameraState.Uninitialized,
            CameraState.Starting,
            CameraState.Ready,
            CameraState.Capturing,
            CameraState.Error("Test error")
        )
        assertEquals(5, states.size)
    }
}
