package com.zirosaur.ziromaps.core.camera

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import com.zirosaur.ziromaps.core.camera.watermark.ExifMetadataWriterImpl
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkData
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
import java.io.FileOutputStream
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExifMetadataWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var writer: ExifMetadataWriterImpl
    private lateinit var jpegFile: File

    @Before
    fun setup() {
        writer = ExifMetadataWriterImpl()
        jpegFile = tempFolder.newFile("test_photo.jpg")

        // Create a valid dummy JPEG
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        FileOutputStream(jpegFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    @Test
    fun writeExifMetadata_writesGpsAndTagsCorrectly() {
        val data = WatermarkData(
            latitude = -7.431250,
            longitude = 109.234510,
            altitudeM = 250.0,
            accuracyM = 3.0f,
            utmCoordinate = "49M 305120m E 9178110m N",
            azimuthDeg = 142.5f,
            timestampEpochMs = 1700000000000L,
            timeZone = TimeZone.getTimeZone("GMT+7"),
            mapProjectName = "Petak 12",
            note = "Patok Batas"
        )

        writer.writeExifMetadata(jpegFile, data)

        val exif = ExifInterface(jpegFile.absolutePath)
        val latLong = exif.latLong

        assertNotNull(latLong)
        assertEquals(-7.431250, latLong!![0], 0.001)
        assertEquals(109.234510, latLong[1], 0.001)

        val altitude = exif.getAltitude(0.0)
        assertEquals(250.0, altitude, 0.5)

        val direction = exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION)
        assertNotNull(direction)
        assertTrue(direction!!.startsWith("14250/100") || direction == "142.5")

        val desc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
        assertNotNull(desc)
        assertTrue(desc!!.contains("Petak 12"))
        assertTrue(desc.contains("Patok Batas"))

        val dateOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        assertNotNull(dateOriginal)
    }

    @Test
    fun writeExifMetadata_nullCoordinates_doesNotCrash() {
        val data = WatermarkData(
            latitude = null,
            longitude = null,
            mapProjectName = "Peta Uji"
        )

        writer.writeExifMetadata(jpegFile, data)
        val exif = ExifInterface(jpegFile.absolutePath)
        assertEquals(null, exif.latLong)
    }
}
