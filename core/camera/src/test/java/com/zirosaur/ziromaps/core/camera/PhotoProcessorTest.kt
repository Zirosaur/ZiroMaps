package com.zirosaur.ziromaps.core.camera

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import com.zirosaur.ziromaps.core.camera.model.CapturedImage
import com.zirosaur.ziromaps.core.camera.watermark.CanvasWatermarkRenderer
import com.zirosaur.ziromaps.core.camera.watermark.ExifMetadataWriterImpl
import com.zirosaur.ziromaps.core.camera.watermark.PhotoProcessorImpl
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoProcessorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var processor: PhotoProcessorImpl

    @Before
    fun setup() {
        val watermarkRenderer = CanvasWatermarkRenderer()
        val exifWriter = ExifMetadataWriterImpl()
        processor = PhotoProcessorImpl(watermarkRenderer, exifWriter)
    }

    @Test
    fun processAndSavePhoto_savesFileWithWatermarkAndExif(): Unit = runBlocking {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val capturedImage = CapturedImage(
            bitmap = bitmap,
            rotationDegrees = 0,
            width = 800,
            height = 600
        )
        val data = WatermarkData(
            latitude = -7.500000,
            longitude = 110.500000,
            altitudeM = 120.0,
            accuracyM = 4.0f,
            utmCoordinate = "49M 450000m E 9170000m N",
            azimuthDeg = 270f,
            mapProjectName = "Survey Area 1",
            note = "Benchmark 01"
        )
        val destFile = File(tempFolder.root, "output/saved_photo.jpg")

        val result = processor.processAndSavePhoto(capturedImage, data, destFile)
        assertTrue(result.isSuccess)

        val savedFile = result.getOrNull()
        assertNotNull(savedFile)
        assertTrue(savedFile!!.exists())
        assertTrue(savedFile.length() > 0)

        // Verify EXIF
        val exif = ExifInterface(savedFile.absolutePath)
        val latLong = exif.latLong
        assertNotNull(latLong)
        assertEquals(-7.5, latLong!![0], 0.001)
        assertEquals(110.5, latLong[1], 0.001)
    }
}
