package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun MapScaleBar(
    metersPerPixel: Double,
    modifier: Modifier = Modifier
) {
    if (metersPerPixel <= 0.0 || metersPerPixel.isNaN() || metersPerPixel.isInfinite()) return

    val density = LocalDensity.current
    val targetBarWidthPx = with(density) { 90.dp.toPx() }

    val (barWidthDp, label) = remember(metersPerPixel, targetBarWidthPx) {
        val targetMeters = targetBarWidthPx * metersPerPixel
        val niceDistanceM = computeNiceDistance(targetMeters)
        val actualWidthPx = (niceDistanceM / metersPerPixel).toFloat()
        val widthDp = (actualWidthPx / density.density).dp.coerceIn(40.dp, 160.dp)

        val formattedLabel = if (niceDistanceM >= 1000.0) {
            val km = niceDistanceM / 1000.0
            if (km % 1.0 == 0.0) "${km.toInt()} km" else String.format(java.util.Locale.US, "%.1f km", km)
        } else {
            "${niceDistanceM.toInt()} m"
        }

        widthDp to formattedLabel
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Canvas(
                modifier = Modifier
                    .width(barWidthDp)
                    .height(6.dp)
            ) {
                val strokeW = 1.5.dp.toPx()
                val tickH = size.height
                val color = Color.Black

                // Base horizontal bar
                drawLine(
                    color = color,
                    start = Offset(0f, tickH),
                    end = Offset(size.width, tickH),
                    strokeWidth = strokeW
                )

                // Left tick
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, tickH),
                    strokeWidth = strokeW
                )

                // Center tick
                drawLine(
                    color = color,
                    start = Offset(size.width / 2f, tickH / 2f),
                    end = Offset(size.width / 2f, tickH),
                    strokeWidth = strokeW
                )

                // Right tick
                drawLine(
                    color = color,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, tickH),
                    strokeWidth = strokeW
                )
            }
        }
    }
}

private fun computeNiceDistance(targetMeters: Double): Double {
    if (targetMeters <= 0.0) return 100.0
    val exponent = floor(log10(targetMeters))
    val fraction = targetMeters / 10.0.pow(exponent)
    val niceFraction = when {
        fraction < 1.5 -> 1.0
        fraction < 3.5 -> 2.0
        fraction < 7.5 -> 5.0
        else -> 10.0
    }
    return niceFraction * 10.0.pow(exponent)
}
