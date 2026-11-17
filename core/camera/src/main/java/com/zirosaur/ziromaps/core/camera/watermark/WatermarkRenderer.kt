package com.zirosaur.ziromaps.core.camera.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface WatermarkRenderer {
    fun renderWatermark(
        source: Bitmap,
        data: WatermarkData,
        inPlace: Boolean = false
    ): Bitmap

    fun generateWatermarkLines(data: WatermarkData): List<String>
}

@Singleton
class CanvasWatermarkRenderer @Inject constructor() : WatermarkRenderer {

    override fun generateWatermarkLines(data: WatermarkData): List<String> {
        val lines = mutableListOf<String>()
        val config = data.config

        // Line 1: Coordinates + Accuracy
        if (config.showCoordinates) {
            val line1 = if (data.latitude != null && data.longitude != null) {
                val latStr = String.format(Locale.US, "%.6f°", data.latitude)
                val lonStr = String.format(Locale.US, "%.6f°", data.longitude)
                val accStr = when {
                    data.accuracyM == null -> ""
                    data.accuracyM <= 30f -> String.format(Locale.US, " (±%.1f m)", data.accuracyM)
                    else -> String.format(Locale.US, " (±%.1f m - Akurasi Rendah)", data.accuracyM)
                }
                "Lat: $latStr  Lon: $lonStr$accStr"
            } else {
                "Lat: -  Lon: - (Mencari GPS)"
            }
            lines.add(line1)
        }

        // Line 2: UTM + Altitude + Azimuth
        val parts = mutableListOf<String>()
        if (config.showUtm) {
            val utmStr = data.utmCoordinate?.ifBlank { null } ?: "-"
            parts.add("UTM: $utmStr")
        }
        if (config.showAltitude && data.altitudeM != null) {
            parts.add(String.format(Locale.US, "Alt: %.0f m", data.altitudeM))
        }
        if (config.showAzimuth) {
            val azimuthStr = CompassDirection.formatAzimuth(data.azimuthDeg)
            parts.add("Azimuth: $azimuthStr")
        }
        if (parts.isNotEmpty()) {
            lines.add(parts.joinToString("  |  "))
        }

        // Line 3: Time
        if (config.showTimestamp) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).apply {
                timeZone = data.timeZone
            }
            val formattedTime = dateFormat.format(Date(data.timestampEpochMs))
            lines.add("Waktu: $formattedTime")
        }

        // Line 4: Map Project Name & Note (Optional)
        val project = if (config.showMapName) data.mapProjectName?.trim()?.ifBlank { null } else null
        val note = if (config.showNote) data.note?.trim()?.ifBlank { null } else null
        when {
            project != null && note != null -> lines.add("Peta: $project | $note")
            project != null -> lines.add("Peta: $project")
            note != null -> lines.add("Catatan: $note")
        }

        if (lines.isEmpty()) {
            lines.add("Ziro Maps Geotag")
        }

        return lines
    }

    override fun renderWatermark(
        source: Bitmap,
        data: WatermarkData,
        inPlace: Boolean
    ): Bitmap {
        val targetBitmap = if (inPlace && source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val canvas = Canvas(targetBitmap)
        val minDim = minOf(targetBitmap.width, targetBitmap.height)
        val scale = (minDim / 1080f).coerceAtLeast(0.5f)

        val textSize = 22f * scale * data.config.textSize.scaleFactor
        val paddingX = 16f * scale
        val paddingY = 14f * scale
        val margin = 24f * scale
        val cornerRadius = 8f * scale
        val lineSpacing = 6f * scale

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(2f * scale, 1f * scale, 1f * scale, Color.argb(180, 0, 0, 0))
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(128, 0, 0, 0) // Semi-transparent #80000000
            style = Paint.Style.FILL
        }

        val lines = generateWatermarkLines(data)
        val maxTextWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.descent - fontMetrics.ascent
        val totalTextHeight = lines.size * lineHeight + (lines.size - 1) * lineSpacing

        val bgWidth = maxTextWidth + 2 * paddingX
        val bgHeight = totalTextHeight + 2 * paddingY

        val (bgLeft, bgTop) = when (data.config.position) {
            WatermarkPosition.BOTTOM_LEFT -> Pair(margin, targetBitmap.height - margin - bgHeight)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(targetBitmap.width - margin - bgWidth, targetBitmap.height - margin - bgHeight)
            WatermarkPosition.TOP_LEFT -> Pair(margin, margin)
        }
        val bgRight = bgLeft + bgWidth
        val bgBottom = bgTop + bgHeight

        val bgRect = RectF(bgLeft, bgTop, bgRight, bgBottom)
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        var textY = bgTop + paddingY - fontMetrics.ascent
        val textX = bgLeft + paddingX

        for (line in lines) {
            canvas.drawText(line, textX, textY, textPaint)
            textY += lineHeight + lineSpacing
        }

        return targetBitmap
    }
}
