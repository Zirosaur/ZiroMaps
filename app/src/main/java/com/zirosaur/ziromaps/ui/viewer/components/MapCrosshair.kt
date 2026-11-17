package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * High-contrast targeting crosshair anchored at the exact center of the map viewport.
 */
@Composable
fun MapCrosshair(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)

            val reticleRadius = 14.dp.toPx()
            val armLength = 10.dp.toPx()
            val innerGap = 4.dp.toPx()

            val strokeWidthDark = 2.5.dp.toPx()
            val strokeWidthLight = 1.2.dp.toPx()

            val darkColor = Color(0x99000000)
            val lightColor = Color(0xFFFFFFFF)

            // 1. Draw outer circle (dark outline, then white inner)
            drawCircle(
                color = darkColor,
                radius = reticleRadius,
                center = center,
                style = Stroke(width = strokeWidthDark)
            )
            drawCircle(
                color = lightColor,
                radius = reticleRadius,
                center = center,
                style = Stroke(width = strokeWidthLight)
            )

            // 2. Draw 4 crosshair arms: Top, Bottom, Left, Right
            val arms = listOf(
                // Top arm
                Offset(cx, cy - reticleRadius - armLength) to Offset(cx, cy - reticleRadius + 2f),
                // Bottom arm
                Offset(cx, cy + reticleRadius - 2f) to Offset(cx, cy + reticleRadius + armLength),
                // Left arm
                Offset(cx - reticleRadius - armLength, cy) to Offset(cx - reticleRadius + 2f, cy),
                // Right arm
                Offset(cx + reticleRadius - 2f, cy) to Offset(cx + reticleRadius + armLength, cy)
            )

            // Shadow / dark outline
            arms.forEach { (start, end) ->
                drawLine(
                    color = darkColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidthDark
                )
            }

            // Foreground white lines
            arms.forEach { (start, end) ->
                drawLine(
                    color = lightColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidthLight
                )
            }

            // 3. Center pinpoint dot
            drawCircle(
                color = darkColor,
                radius = 2.2.dp.toPx(),
                center = center
            )
            drawCircle(
                color = lightColor,
                radius = 1.2.dp.toPx(),
                center = center
            )
        }
    }
}
