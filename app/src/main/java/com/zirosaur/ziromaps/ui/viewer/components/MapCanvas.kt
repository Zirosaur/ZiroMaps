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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.core.database.model.TrackWithPoints
import com.zirosaur.ziromaps.core.database.model.WaypointWithPhotos
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.geopdf.model.PagePoint
import com.zirosaur.ziromaps.core.geopdf.model.TransformMatrix
import com.zirosaur.ziromaps.core.geopdf.tile.MapTile
import com.zirosaur.ziromaps.ui.viewer.GpsFollowMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MapCanvas(
    mapId: String,
    pageWidthPt: Double,
    pageHeightPt: Double,
    transformMatrix: TransformMatrix?,
    userPagePoint: PagePoint?,
    userAccuracyM: Float?,
    userAzimuthDeg: Float?,
    followMode: GpsFollowMode = GpsFollowMode.FREE,
    rotationDeg: Float = 0f,
    panToLatLon: LatLon? = null,
    centerGpsTrigger: Long = 0L,
    waypoints: List<WaypointWithPhotos> = emptyList(),
    tracks: List<TrackWithPoints> = emptyList(),
    onWaypointClick: (WaypointWithPhotos) -> Unit = {},
    onMapCenterChanged: (LatLon?) -> Unit = {},
    onMetersPerPixelChanged: (Double) -> Unit = {},
    onUserPan: () -> Unit = {},
    loadTileBitmap: suspend (MapTile) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(0.25f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialCentered by remember { mutableStateOf(false) }

    val density = LocalDensity.current

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

    // Update map center coordinate and metersPerPixel
    LaunchedEffect(offset, scale, viewportSize, transformMatrix) {
        if (viewportSize.width > 0 && viewportSize.height > 0 && transformMatrix != null) {
            val centerPtX = (viewportSize.width / 2f - offset.x) / scale
            val centerPtY = (viewportSize.height / 2f - offset.y) / scale
            val centerLatLon = transformMatrix.toLatLon(centerPtX.toDouble(), centerPtY.toDouble())
            onMapCenterChanged(centerLatLon)

            // Compute meters per pixel
            val offsetPtX = centerPtX.toDouble() + 50.0
            val offsetLatLon = transformMatrix.toLatLon(offsetPtX, centerPtY.toDouble())
            val dLat = Math.toRadians(offsetLatLon.latitude - centerLatLon.latitude)
            val dLon = Math.toRadians(offsetLatLon.longitude - centerLatLon.longitude)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(centerLatLon.latitude)) * cos(Math.toRadians(offsetLatLon.latitude)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val distM = 6371000.0 * c
            val metersPerPt = distM / 50.0
            val metersPerPixel = metersPerPt / scale
            onMetersPerPixelChanged(metersPerPixel)
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

    // React to Follow Mode updates
    LaunchedEffect(userPagePoint, followMode, viewportSize) {
        if ((followMode == GpsFollowMode.FOLLOW_LOCATION || followMode == GpsFollowMode.HEADING) &&
            userPagePoint != null && viewportSize.width > 0
        ) {
            val targetX = (viewportSize.width / 2f) - (userPagePoint.xPt.toFloat() * scale)
            val targetY = (viewportSize.height / 2f) - (userPagePoint.yPt.toFloat() * scale)
            offset = Offset(targetX, targetY)
        }
    }

    // Pan to specific LatLon (e.g. from Layers list selection)
    LaunchedEffect(panToLatLon) {
        if (panToLatLon != null && transformMatrix != null && viewportSize.width > 0) {
            val targetPt = transformMatrix.toPagePoint(panToLatLon.latitude, panToLatLon.longitude)
            val targetX = (viewportSize.width / 2f) - (targetPt.xPt.toFloat() * scale)
            val targetY = (viewportSize.height / 2f) - (targetPt.yPt.toFloat() * scale)
            offset = Offset(targetX, targetY)
        }
    }

    // Determine current zoom level for tile pyramid
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

        // Generous margin for smooth panning and rotation
        val marginPx = 250f
        val xMinPt = ((-offset.x - marginPx) / scale).coerceAtLeast(0f)
        val yMinPt = ((-offset.y - marginPx) / scale).coerceAtLeast(0f)
        val xMaxPt = (((viewportSize.width - offset.x) + marginPx) / scale).coerceAtMost(pageWidthPt.toFloat())
        val yMaxPt = (((viewportSize.height - offset.y) + marginPx) / scale).coerceAtMost(pageHeightPt.toFloat())

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
                    if (pan.x != 0f || pan.y != 0f) {
                        onUserPan()
                    }
                    val oldScale = scale
                    val newScale = (oldScale * zoom).coerceIn(0.1f, 10.0f)
                    val factor = newScale / oldScale
                    offset = (offset - centroid) * factor + centroid + pan
                    scale = newScale
                }
            }
            .pointerInput(waypoints, transformMatrix, scale, offset) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val newScale = (scale * 2.0f).coerceIn(0.1f, 10.0f)
                        val factor = newScale / scale
                        offset = (offset - tapOffset) * factor + tapOffset
                        scale = newScale
                    },
                    onTap = { tapOffset ->
                        // Check if tap hit any waypoint pin (radius ~28 dp)
                        if (transformMatrix != null) {
                            val hitRadiusPx = with(density) { 28.dp.toPx() }
                            for (item in waypoints) {
                                val pt = transformMatrix.toPagePoint(item.waypoint.latitude, item.waypoint.longitude)
                                val pinX = (pt.xPt.toFloat() * scale) + offset.x
                                val pinY = (pt.yPt.toFloat() * scale) + offset.y
                                val dist = sqrt((tapOffset.x - pinX) * (tapOffset.x - pinX) + (tapOffset.y - pinY) * (tapOffset.y - pinY))
                                if (dist <= hitRadiusPx) {
                                    onWaypointClick(item)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                rotationZ = rotationDeg
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

            // 3. Draw Track Polylines
            if (transformMatrix != null) {
                tracks.forEach { trackWithPoints ->
                    val pts = trackWithPoints.points
                    if (pts.size >= 2) {
                        val trackPath = Path()
                        val p0 = transformMatrix.toPagePoint(pts[0].latitude, pts[0].longitude)
                        val startX = (p0.xPt.toFloat() * scale) + offset.x
                        val startY = (p0.yPt.toFloat() * scale) + offset.y
                        trackPath.moveTo(startX, startY)

                        for (i in 1 until pts.size) {
                            val pi = transformMatrix.toPagePoint(pts[i].latitude, pts[i].longitude)
                            val px = (pi.xPt.toFloat() * scale) + offset.x
                            val py = (pi.yPt.toFloat() * scale) + offset.y
                            trackPath.lineTo(px, py)
                        }

                        // Glow border
                        drawPath(
                            path = trackPath,
                            color = Color(0x66FF6F00),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Core track line
                        drawPath(
                            path = trackPath,
                            color = Color(0xFFFF8F00),
                            style = Stroke(width = 3.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // 4. Draw Waypoint Pins
            if (transformMatrix != null) {
                waypoints.forEach { item ->
                    val wp = item.waypoint
                    val pt = transformMatrix.toPagePoint(wp.latitude, wp.longitude)
                    val pinX = (pt.xPt.toFloat() * scale) + offset.x
                    val pinY = (pt.yPt.toFloat() * scale) + offset.y

                    val pinColor = if (wp.hasPhoto) Color(0xFF8E24AA) else Color(0xFFE53935)

                    // Pin shadow
                    drawCircle(
                        color = Color(0x40000000),
                        radius = 8.dp.toPx(),
                        center = Offset(pinX, pinY + 2.dp.toPx())
                    )

                    // Pin Outer Body
                    drawCircle(
                        color = pinColor,
                        radius = 7.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )

                    // Pin Inner Center
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )
                }
            }

            // 5. Draw GPS Blue Dot & Orientation Cone
            if (userPagePoint != null) {
                val dotX = (userPagePoint.xPt.toFloat() * scale) + offset.x
                val dotY = (userPagePoint.yPt.toFloat() * scale) + offset.y

                val accuracyM = userAccuracyM ?: 10f
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
