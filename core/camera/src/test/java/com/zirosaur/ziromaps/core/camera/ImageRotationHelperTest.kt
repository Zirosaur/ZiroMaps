package com.zirosaur.ziromaps.core.camera

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import com.zirosaur.ziromaps.core.camera.util.ImageRotationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageRotationHelperTest {

    @Test
    fun rotateBitmap_zeroDegrees_returnsOriginalInstance() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val result = ImageRotationHelper.rotateBitmap(bitmap, 0)
        assertSame(bitmap, result)
        assertEquals(100, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun rotateBitmap_90Degrees_swapsDimensions() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val result = ImageRotationHelper.rotateBitmap(bitmap, 90)
        assertNotSame(bitmap, result)
        assertEquals(200, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun rotateBitmap_180Degrees_preservesDimensions() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val result = ImageRotationHelper.rotateBitmap(bitmap, 180)
        assertNotSame(bitmap, result)
        assertEquals(100, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun rotateBitmap_270Degrees_swapsDimensions() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val result = ImageRotationHelper.rotateBitmap(bitmap, 270)
        assertNotSame(bitmap, result)
        assertEquals(200, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun mirrorBitmap_preservesDimensions() {
        val bitmap = Bitmap.createBitmap(150, 300, Bitmap.Config.ARGB_8888)
        val result = ImageRotationHelper.mirrorBitmap(bitmap)
        assertNotSame(bitmap, result)
        assertEquals(150, result.width)
        assertEquals(300, result.height)
    }

    @Test
    fun degreesToExifOrientation_normalAndMirroredMappings() {
        assertEquals(ExifInterface.ORIENTATION_NORMAL, ImageRotationHelper.degreesToExifOrientation(0, false))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, ImageRotationHelper.degreesToExifOrientation(90, false))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_180, ImageRotationHelper.degreesToExifOrientation(180, false))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_270, ImageRotationHelper.degreesToExifOrientation(270, false))

        assertEquals(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, ImageRotationHelper.degreesToExifOrientation(0, true))
        assertEquals(ExifInterface.ORIENTATION_TRANSPOSE, ImageRotationHelper.degreesToExifOrientation(90, true))
        assertEquals(ExifInterface.ORIENTATION_FLIP_VERTICAL, ImageRotationHelper.degreesToExifOrientation(180, true))
        assertEquals(ExifInterface.ORIENTATION_TRANSVERSE, ImageRotationHelper.degreesToExifOrientation(270, true))
    }
}
