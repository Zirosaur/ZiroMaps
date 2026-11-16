package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.core.geopdf.model.LatLon
import com.zirosaur.ziromaps.core.location.converter.CoordinateFormatter
import com.zirosaur.ziromaps.core.location.converter.UtmConverter
import com.zirosaur.ziromaps.core.location.model.UserLocation

enum class WaypointLocationSource {
    GPS,
    MAP_CENTER
}

@Composable
fun AddWaypointDialog(
    defaultTitle: String,
    gpsLocation: UserLocation?,
    mapCenterLatLon: LatLon?,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, lat: Double, lon: Double, alt: Double?, acc: Float?) -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle) }
    var note by remember { mutableStateOf("") }
    var selectedSource by remember {
        mutableStateOf(
            if (gpsLocation != null) WaypointLocationSource.GPS else WaypointLocationSource.MAP_CENTER
        )
    }

    val targetLat = if (selectedSource == WaypointLocationSource.GPS && gpsLocation != null) {
        gpsLocation.latitude
    } else {
        mapCenterLatLon?.latitude ?: gpsLocation?.latitude ?: 0.0
    }

    val targetLon = if (selectedSource == WaypointLocationSource.GPS && gpsLocation != null) {
        gpsLocation.longitude
    } else {
        mapCenterLatLon?.longitude ?: gpsLocation?.longitude ?: 0.0
    }

    val targetAlt = if (selectedSource == WaypointLocationSource.GPS) gpsLocation?.altitudeM else null
    val targetAcc = if (selectedSource == WaypointLocationSource.GPS) gpsLocation?.accuracyM else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.add_waypoint_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Radio buttons for coordinate source
                if (gpsLocation != null && mapCenterLatLon != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSource == WaypointLocationSource.GPS,
                                onClick = { selectedSource = WaypointLocationSource.GPS }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == WaypointLocationSource.GPS,
                            onClick = { selectedSource = WaypointLocationSource.GPS }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.use_gps_position),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSource == WaypointLocationSource.MAP_CENTER,
                                onClick = { selectedSource = WaypointLocationSource.MAP_CENTER }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == WaypointLocationSource.MAP_CENTER,
                            onClick = { selectedSource = WaypointLocationSource.MAP_CENTER }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.use_map_center),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Coordinate info preview
                val utm = UtmConverter.latLonToUtm(targetLat, targetLon)
                Text(
                    text = utm.toFormattedString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = CoordinateFormatter.toDmsString(targetLat, targetLon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.waypoint_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(id = R.string.waypoint_note_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTitle = title.ifBlank { defaultTitle }
                    onSave(finalTitle, note, targetLat, targetLon, targetAlt, targetAcc)
                }
            ) {
                Text(stringResource(id = R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_button))
            }
        }
    )
}
