package com.zirosaur.ziromaps.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirosaur.ziromaps.core.camera.watermark.CompassDirection
import com.zirosaur.ziromaps.ui.camera.CameraStampUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveWatermarkOverlay(
    uiState: CameraStampUiState,
    modifier: Modifier = Modifier
) {
    val config = uiState.watermarkConfig
    val loc = uiState.currentLocation
    val utm = uiState.currentUtm
    val compass = uiState.currentCompass
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()) }
    val currentTimeStr = remember(System.currentTimeMillis()) { timeFormatter.format(Date()) }

    val baseFontSize = (10f * config.textSize.scaleFactor).sp

    val lines = buildList {
        // Line 1: Lat, Lon, Accuracy
        if (config.showLatLon) {
            val line1 = if (loc != null) {
                val acc = loc.accuracyM
                val accText = when {
                    acc == null -> ""
                    acc <= 30f -> String.format(Locale.US, " (±%.1f m)", acc)
                    else -> String.format(Locale.US, " (±%.1f m - Akurasi Rendah)", acc)
                }
                String.format(Locale.US, "Lat: %.6f°  Lon: %.6f°%s", loc.latitude, loc.longitude, accText)
            } else {
                "Lat: -  Lon: - (Mencari GPS…)"
            }
            add(line1)
        }

        // Line 2: UTM & Azimuth
        val line2Parts = mutableListOf<String>()
        if (config.showUtm) {
            val utmStr = utm?.toFormattedString() ?: "-"
            line2Parts.add("UTM: $utmStr")
        }
        if (config.showAzimuth) {
            val azimuthStr = CompassDirection.formatAzimuth(compass?.azimuthDeg)
            line2Parts.add("Azimuth: $azimuthStr")
        }
        if (line2Parts.isNotEmpty()) {
            add(line2Parts.joinToString("  |  "))
        }

        // Altitude
        if (config.showAltitude && loc?.altitudeM != null) {
            add(String.format(Locale.US, "Elevasi: %.1f m dpl", loc.altitudeM))
        }

        // Timestamp
        if (config.showTimestamp) {
            add("Waktu: $currentTimeStr")
        }

        // Map Name & Quick Note
        val project = if (config.showMapName) uiState.projectName.trim().ifBlank { null } else null
        val note = if (config.showNote) uiState.quickNote.trim().ifBlank { null } else null
        if (project != null || note != null) {
            val line = when {
                project != null && note != null -> "Peta: $project | $note"
                project != null -> "Peta: $project"
                else -> "Catatan: $note"
            }
            add(line)
        }
    }

    if (lines.isEmpty()) return

    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column {
            lines.forEachIndexed { index, line ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = line,
                    color = Color.White,
                    fontSize = baseFontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
