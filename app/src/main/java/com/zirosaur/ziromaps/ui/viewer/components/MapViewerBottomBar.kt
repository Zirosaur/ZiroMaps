package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.ui.viewer.GpsFollowMode
import java.util.Locale

@Composable
fun MapViewerBottomBar(
    followMode: GpsFollowMode,
    isRecordingTrack: Boolean,
    recordedDistanceM: Double,
    recordingDurationSec: Long,
    onCycleFollowMode: () -> Unit,
    onAddPinClick: () -> Unit,
    onRecordTrackClick: () -> Unit,
    onLayersClick: () -> Unit,
    onCameraStampClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left side quick action buttons in an ergonomic dock
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Cycle GPS Follow Mode (Free -> Follow -> Heading)
                val gpsContainerColor by animateColorAsState(
                    targetValue = when (followMode) {
                        GpsFollowMode.FREE -> MaterialTheme.colorScheme.surface
                        GpsFollowMode.FOLLOW_LOCATION -> MaterialTheme.colorScheme.primaryContainer
                        GpsFollowMode.HEADING -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    label = "gpsColor"
                )
                val gpsContentColor = when (followMode) {
                    GpsFollowMode.FREE -> MaterialTheme.colorScheme.onSurface
                    GpsFollowMode.FOLLOW_LOCATION -> MaterialTheme.colorScheme.primary
                    GpsFollowMode.HEADING -> MaterialTheme.colorScheme.tertiary
                }

                SmallFloatingActionButton(
                    onClick = onCycleFollowMode,
                    containerColor = gpsContainerColor,
                    contentColor = gpsContentColor,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (followMode == GpsFollowMode.HEADING) R.drawable.ic_compass else R.drawable.ic_my_location
                        ),
                        contentDescription = "Mode GPS: $followMode",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Add Pin Button
                SmallFloatingActionButton(
                    onClick = onAddPinClick,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pin),
                        contentDescription = stringResource(id = R.string.add_pin),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. Record Track Button / Live Chip
                if (isRecordingTrack) {
                    // Extended live recording pill with timer and distance
                    val hours = recordingDurationSec / 3600
                    val minutes = (recordingDurationSec % 3600) / 60
                    val seconds = recordingDurationSec % 60
                    val timerStr = if (hours > 0) {
                        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    }
                    val km = recordedDistanceM / 1000.0

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFC62828))
                            .clickable(onClick = onRecordTrackClick)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsing red indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timerStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f km", km),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    SmallFloatingActionButton(
                        onClick = onRecordTrackClick,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_track),
                            contentDescription = stringResource(id = R.string.record_track),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. Layers / List Button
                SmallFloatingActionButton(
                    onClick = onLayersClick,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Lapisan & Objek Peta",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Prominent Camera Stamp button on the right
        FloatingActionButton(
            onClick = onCameraStampClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = stringResource(id = R.string.camera_stamp),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
