package com.zirosaur.ziromaps.core.camera

import android.graphics.Bitmap
import com.zirosaur.ziromaps.core.camera.watermark.CanvasWatermarkRenderer
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CanvasWatermarkRendererTest {

    private lateinit var renderer: CanvasWatermarkRenderer

    @Before
    fun setup() {
        renderer = CanvasWatermarkRenderer()
    }

    @Test
    fun generateWatermarkLines_normalGps_containsAllInfo() {
        val data = WatermarkData(
            latitude = -7.431250,
            longitude = 109.234510,
            altitudeM = 150.0,
            accuracyM = 4.2f,
            utmCoordinate = "49M 305120m E 9178110m N",
            azimuthDeg = 142f,
            timestampEpochMs = 1700000000000L,
            timeZone = TimeZone.getTimeZone("GMT+7"),
            mapProjectName = "Hutan Lindung A",
            note = "Pohon Bengkirai"
        )

        val lines = renderer.generateWatermarkLines(data)
        assertEquals(4, lines.size)
        assertTrue(lines[0].contains("Lat: -7.431250°"))
        assertTrue(lines[0].contains("Lon: 109.234510°"))
        assertTrue(lines[0].contains("(±4.2 m)"))
        assertTrue(lines[1].contains("UTM: 49M 305120m E 9178110m N"))
        assertTrue(lines[1].contains("Azimuth: 142° SE"))
        assertTrue(lines[2].startsWith("Waktu:"))
        assertTrue(lines[3].contains("Peta: Hutan Lindung A | Pohon Bengkirai"))
    }

    @Test
    fun generateWatermarkLines_lowAccuracy_marksLowAccuracy() {
        val data = WatermarkData(
            latitude = -7.431250,
            longitude = 109.234510,
            accuracyM = 45.0f
        )

        val lines = renderer.generateWatermarkLines(data)
        assertTrue(lines[0].contains("Akurasi Rendah"))
    }

    @Test
    fun generateWatermarkLines_noGps_showsSearchingLabel() {
        val data = WatermarkData(
            latitude = null,
            longitude = null
        )

        val lines = renderer.generateWatermarkLines(data)
        assertTrue(lines[0].contains("Mencari GPS"))
        assertTrue(lines[1].contains("UTM: -"))
    }

    @Test
    fun generateWatermarkLines_onlyProjectName_omitsNoteSeparator() {
        val data = WatermarkData(
            latitude = -7.1,
            longitude = 110.1,
            mapProjectName = "Proyek X"
        )

        val lines = renderer.generateWatermarkLines(data)
        assertEquals(4, lines.size)
        assertEquals("Peta: Proyek X", lines[3])
    }

    @Test
    fun generateWatermarkLines_noProjectNoNote_threeLines() {
        val data = WatermarkData(
            latitude = -7.1,
            longitude = 110.1
        )

        val lines = renderer.generateWatermarkLines(data)
        assertEquals(3, lines.size)
    }

    @Test
    fun renderWatermark_drawsOnBitmapSuccessfully() {
        val source = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val data = WatermarkData(
            latitude = -7.431250,
            longitude = 109.234510,
            accuracyM = 3.5f,
            utmCoordinate = "49M 305120m E 9178110m N",
            azimuthDeg = 90f,
            mapProjectName = "Test Map"
        )

        val result = renderer.renderWatermark(source, data, inPlace = false)
        assertNotNull(result)
        assertEquals(1920, result.width)
        assertEquals(1080, result.height)
    }
}
