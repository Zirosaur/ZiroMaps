package com.zirosaur.ziromaps.core.camera.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

object ImageRotationHelper {

    /**
     * Rotates a [Bitmap] by [degrees] (normalized to 0, 90, 180, 270).
     * If degrees % 360 == 0, returns the original [source].
     * If [recycleSource] is true, the original bitmap is recycled if a new one is allocated.
     */
    fun rotateBitmap(
        source: Bitmap,
        degrees: Int,
        recycleSource: Boolean = false
    ): Bitmap {
        val normalized = (degrees % 360 + 360) % 360
        if (normalized == 0) return source

        val matrix = Matrix().apply {
            postRotate(normalized.toFloat())
        }

        val rotated = Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )

        if (recycleSource && rotated != source && !source.isRecycled) {
            source.recycle()
        }

        return rotated
    }

    /**
     * Mirrors a [Bitmap] horizontally (typically for front-facing camera preview/capture).
     */
    fun mirrorBitmap(
        source: Bitmap,
        recycleSource: Boolean = false
    ): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }

        val mirrored = Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )

        if (recycleSource && mirrored != source && !source.isRecycled) {
            source.recycle()
        }

        return mirrored
    }

    /**
     * Converts rotation degrees to AndroidX [ExifInterface] orientation constant.
     */
    fun degreesToExifOrientation(degrees: Int, isMirrored: Boolean = false): Int {
        val normalized = (degrees % 360 + 360) % 360
        return if (!isMirrored) {
            when (normalized) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }
        } else {
            when (normalized) {
                90 -> ExifInterface.ORIENTATION_TRANSPOSE
                180 -> ExifInterface.ORIENTATION_FLIP_VERTICAL
                270 -> ExifInterface.ORIENTATION_TRANSVERSE
                else -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
            }
        }
    }
}
