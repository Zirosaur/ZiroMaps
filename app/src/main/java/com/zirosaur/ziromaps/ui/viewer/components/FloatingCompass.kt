package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Floating compass button that indicates true North and resets map rotation on tap.
 */
@Composable
fun FloatingCompass(
    rotationDeg: Float,
    onResetNorth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onResetNorth)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val needleHalfLen = size.height * 0.42f
                val needleHalfWidth = size.width * 0.18f

                // Rotate needle so that red pointer aligns with North
                rotate(degrees = -rotationDeg, pivot = Offset(cx, cy)) {
                    // North arrow (Red)
                    val northPath = Path().apply {
                        moveTo(cx, cy - needleHalfLen)
                        lineTo(cx + needleHalfWidth, cy)
                        lineTo(cx, cy - 2.dp.toPx())
                        lineTo(cx - needleHalfWidth, cy)
                        close()
                    }
                    drawPath(northPath, color = Color(0xFFE53935))

                    // South arrow (White / Slate)
                    val southPath = Path().apply {
                        moveTo(cx, cy + needleHalfLen)
                        lineTo(cx + needleHalfWidth, cy)
                        lineTo(cx, cy + 2.dp.toPx())
                        lineTo(cx - needleHalfWidth, cy)
                        close()
                    }
                    drawPath(southPath, color = Color(0xFF78909C))

                    // Center pivot dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.DarkGray,
                        radius = 1.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}
