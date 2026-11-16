package com.zirosaur.ziromaps.core.camera.watermark

import android.graphics.Bitmap
import com.zirosaur.ziromaps.core.camera.model.CapturedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface PhotoProcessor {
    suspend fun processAndSavePhoto(
        capturedImage: CapturedImage,
        watermarkData: WatermarkData,
        destinationFile: File,
        jpegQuality: Int = 95
    ): Result<File>
}

@Singleton
class PhotoProcessorImpl @Inject constructor(
    private val watermarkRenderer: WatermarkRenderer,
    private val exifMetadataWriter: ExifMetadataWriter
) : PhotoProcessor {

    override suspend fun processAndSavePhoto(
        capturedImage: CapturedImage,
        watermarkData: WatermarkData,
        destinationFile: File,
        jpegQuality: Int
    ): Result<File> = withContext(Dispatchers.Default) {
        try {
            destinationFile.parentFile?.mkdirs()

            // 1. Render Watermark via Canvas
            val watermarkedBitmap = watermarkRenderer.renderWatermark(
                source = capturedImage.bitmap,
                data = watermarkData,
                inPlace = false
            )

            // 2. Compress & write JPEG to destination file
            FileOutputStream(destinationFile).use { out ->
                val success = watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                if (!success) {
                    throw IllegalStateException("Gagal melakukan kompresi JPEG")
                }
                out.flush()
            }

            // Recycle intermediate watermarked bitmap if different from source
            if (watermarkedBitmap != capturedImage.bitmap && !watermarkedBitmap.isRecycled) {
                watermarkedBitmap.recycle()
            }

            // 3. Write EXIF GPS & metadata
            exifMetadataWriter.writeExifMetadata(destinationFile, watermarkData)

            Result.success(destinationFile)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
