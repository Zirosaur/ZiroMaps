package com.zirosaur.ziromaps.ui.viewer.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.tile.MapTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MapCanvas(
    mapId: String,
    pageWidthPt: Double,
    pageHeightPt: Double,
    userPagePoint: PagePoint?,
    userAccuracyM: Float?,
    userAzimuthDeg: Float?,
    centerGpsTrigger: Long,
    loadTileBitmap: suspend (MapTile) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(0.25f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialCentered by remember { mutableStateOf(false) }

    // Memory cache of loaded bitmaps for immediate rendering
    val tilesMap = remember { mutableStateMapOf<String, Bitmap>() }

    // Center map on initial layout
    LaunchedEffect(viewportSize, pageWidthPt, pageHeightPt) {
        if (viewportSize.width > 0 && viewportSize.height > 0 && !isInitialCentered) {
            val scaleX = viewportSize.width.toFloat() / pageWidthPt.toFloat()
            val scaleY = viewportSize.height.toFloat() / pageHeightPt.toFloat()
            scale = minOf(scaleX, scaleY).coerceIn(0.1f, 5.0f)
            val initialX = (viewportSize.width - (pageWidthPt.toFloat() * scale)) / 2f
            val initialY = (viewportSize.height - (pageHeightPt.toFloat() * scale)) / 2f
            offset = Offset(initialX, initialY)
            isInitialCentered = true
        }
    }

    // React to Center GPS trigger
    LaunchedEffect(centerGpsTrigger) {
        if (centerGpsTrigger > 0L && userPagePoint != null && viewportSize.width > 0) {
            val targetX = (viewportSize.width / 2f) - (userPagePoint.xPt.toFloat() * scale)
            val targetY = (viewportSize.height / 2f) - (userPagePoint.yPt.toFloat() * scale)
            offset = Offset(targetX, targetY)
        }
    }

    // Determine current zoom level for tile pyramid
    // Zoom 0 = 0.25x, Zoom 1 = 0.5x, Zoom 2 = 1.0x, Zoom 3 = 2.0x, Zoom 4 = 4.0x, Zoom 5 = 8.0x
    val zoomLevel = remember(scale) {
        val calculated = floor(ln(scale.toDouble() / 0.25) / ln(2.0)).toInt()
        calculated.coerceIn(0, 5)
    }
    val tileScale = remember(zoomLevel) { MapTile.calculateScale(zoomLevel) }
    val tileSize = MapTile.DEFAULT_TILE_SIZE
    val tileSpanPt = remember(tileScale) { tileSize / tileScale }

    // Compute visible tile ranges
    val visibleTiles = remember(offset, scale, viewportSize, pageWidthPt, pageHeightPt, zoomLevel) {
        if (viewportSize.width == 0 || viewportSize.height == 0) return@remember emptyList()

        val xMinPt = (-offset.x / scale).coerceAtLeast(0f)
        val yMinPt = (-offset.y / scale).coerceAtLeast(0f)
        val xMaxPt = ((viewportSize.width - offset.x) / scale).coerceAtMost(pageWidthPt.toFloat())
        val yMaxPt = ((viewportSize.height - offset.y) / scale).coerceAtMost(pageHeightPt.toFloat())

        val minTileX = (xMinPt / tileSpanPt).toInt().coerceAtLeast(0)
        val maxTileX = (xMaxPt / tileSpanPt).toInt()
            .coerceAtMost(ceil(pageWidthPt * tileScale / tileSize).toInt() - 1)

        val minTileY = (yMinPt / tileSpanPt).toInt().coerceAtLeast(0)
        val maxTileY = (yMaxPt / tileSpanPt).toInt()
            .coerceAtMost(ceil(pageHeightPt * tileScale / tileSize).toInt() - 1)

        val list = mutableListOf<MapTile>()
        for (ty in minTileY..maxTileY) {
            for (tx in minTileX..maxTileX) {
                list.add(
                    MapTile(
                        mapId = mapId,
                        zoom = zoomLevel,
                        tileX = tx,
                        tileY = ty,
                        tileSize = tileSize,
                        scale = tileScale
                    )
                )
            }
        }
        list
    }

    // Asynchronously fetch visible tiles
    LaunchedEffect(visibleTiles) {
        withContext(Dispatchers.IO) {
            visibleTiles.forEach { tile ->
                if (!tilesMap.containsKey(tile.cacheKey)) {
                    val bitmap = loadTileBitmap(tile)
                    if (bitmap != null) {
                        tilesMap[tile.cacheKey] = bitmap
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim)
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (oldScale * zoom).coerceIn(0.1f, 10.0f)
                    val factor = newScale / oldScale
                    offset = (offset - centroid) * factor + centroid + pan
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val newScale = (scale * 2.0f).coerceIn(0.1f, 10.0f)
                        val factor = newScale / scale
                        offset = (offset - tapOffset) * factor + tapOffset
                        scale = newScale
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw Map Tiles
            visibleTiles.forEach { tile ->
                val tileLeft = (tile.tileX * tileSpanPt * scale) + offset.x
                val tileTop = (tile.tileY * tileSpanPt * scale) + offset.y
                val tileDimension = tileSpanPt * scale

                val bitmap = tilesMap[tile.cacheKey]
                if (bitmap != null && !bitmap.isRecycled) {
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(tileLeft.roundToInt(), tileTop.roundToInt()),
                        dstSize = IntSize(tileDimension.roundToInt(), tileDimension.roundToInt())
                    )
                } else {
                    // Placeholder background for pending tile
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(tileLeft, tileTop),
                        size = Size(tileDimension, tileDimension)
                    )
                }
            }

            // 2. Draw Map Paper Border
            drawRect(
                color = Color.LightGray,
                topLeft = offset,
                size = Size(pageWidthPt.toFloat() * scale, pageHeightPt.toFloat() * scale),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Draw GPS Blue Dot & Orientation Cone
            if (userPagePoint != null) {
                val dotX = (userPagePoint.xPt.toFloat() * scale) + offset.x
                val dotY = (userPagePoint.yPt.toFloat() * scale) + offset.y

                // Estimated accuracy radius in screen pixels
                val accuracyM = userAccuracyM ?: 10f
                // Standard scale: 1 point ~ 0.35 mm; 1m is roughly 0.5 to 2 pt depending on map scale.
                // We estimate ~ 1.5 pt per meter as general indicator
                val accuracyRadiusPt = accuracyM * 1.5f
                val accuracyRadiusPx = (accuracyRadiusPt * scale).coerceIn(12.dp.toPx(), 120.dp.toPx())

                // Accuracy circle
                drawCircle(
                    color = Color(0x332196F3),
                    radius = accuracyRadiusPx,
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = Color(0x661976D2),
                    radius = accuracyRadiusPx,
                    center = Offset(dotX, dotY),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Azimuth directional cone (if compass available)
                if (userAzimuthDeg != null) {
                    val azimuthRad = Math.toRadians((userAzimuthDeg - 90.0)).toFloat()
                    val coneAngleSpan = Math.toRadians(35.0).toFloat()
                    val coneLength = 36.dp.toPx()

                    val leftRad = azimuthRad - coneAngleSpan
                    val rightRad = azimuthRad + coneAngleSpan

                    val conePath = Path().apply {
                        moveTo(dotX, dotY)
                        lineTo(dotX + coneLength * cos(leftRad), dotY + coneLength * sin(leftRad))
                        lineTo(dotX + coneLength * cos(rightRad), dotY + coneLength * sin(rightRad))
                        close()
                    }

                    drawPath(
                        path = conePath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x662196F3), Color(0x002196F3)),
                            center = Offset(dotX, dotY),
                            radius = coneLength
                        )
                    )
                }

                // White outer halo
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = Offset(dotX, dotY)
                )

                // Solid blue core
                drawCircle(
                    color = Color(0xFF1976D2),
                    radius = 7.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}
