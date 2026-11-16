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
    val loc = uiState.currentLocation
    val utm = uiState.currentUtm
    val compass = uiState.currentCompass
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()) }
    val currentTimeStr = remember(System.currentTimeMillis()) { timeFormatter.format(Date()) }

    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            // Line 1: Lat, Lon, Accuracy
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
            Text(
                text = line1,
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Line 2: UTM & Azimuth
            val utmStr = utm?.toFormattedString() ?: "-"
            val azimuthStr = CompassDirection.formatAzimuth(compass?.azimuthDeg)
            Text(
                text = "UTM: $utmStr  |  Azimuth: $azimuthStr",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Line 3: Time
            Text(
                text = "Waktu: $currentTimeStr",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Line 4: Project & Note (if available)
            val project = uiState.projectName.trim().ifBlank { null }
            val note = uiState.quickNote.trim().ifBlank { null }
            if (project != null || note != null) {
                Spacer(modifier = Modifier.height(2.dp))
                val line4 = when {
                    project != null && note != null -> "Peta: $project | $note"
                    project != null -> "Peta: $project"
                    else -> "Catatan: $note"
                }
                Text(
                    text = line4,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
