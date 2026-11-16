package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.R

@Composable
fun MapViewerBottomBar(
    isRecordingTrack: Boolean,
    recordedDistanceM: Double,
    onCenterGpsClick: () -> Unit,
    onAddPinClick: () -> Unit,
    onRecordTrackClick: () -> Unit,
    onCameraStampClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left side quick action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Center GPS button
            SmallFloatingActionButton(
                onClick = onCenterGpsClick,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_my_location),
                    contentDescription = stringResource(id = R.string.center_gps),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Add Pin button
            SmallFloatingActionButton(
                onClick = onAddPinClick,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pin),
                    contentDescription = stringResource(id = R.string.add_pin),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Record Track button
            val trackContainerColor = if (isRecordingTrack) Color(0xFFC62828) else MaterialTheme.colorScheme.surface
            val trackContentColor = if (isRecordingTrack) Color.White else MaterialTheme.colorScheme.onSurface

            SmallFloatingActionButton(
                onClick = onRecordTrackClick,
                containerColor = trackContainerColor,
                contentColor = trackContentColor,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_track),
                        contentDescription = if (isRecordingTrack) stringResource(id = R.string.stop_track) else stringResource(id = R.string.record_track),
                        modifier = Modifier.size(20.dp)
                    )
                    if (isRecordingTrack) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f km", recordedDistanceM / 1000.0),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Prominent Camera Stamp button on the right
        FloatingActionButton(
            onClick = onCameraStampClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = stringResource(id = R.string.camera_stamp),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
