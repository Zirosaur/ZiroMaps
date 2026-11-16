package com.zirosaur.ziromaps.ui.viewer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.core.export.model.ExportFormat

@Composable
fun ExportDialog(
    projectName: String,
    waypointCount: Int,
    trackCount: Int,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.KMZ) }

    AlertDialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.export_project_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$projectName ($waypointCount waypoint, $trackCount track)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.export_format_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExportFormat.entries.forEach { format ->
                    val isSelected = format == selectedFormat
                    val (title, description) = when (format) {
                        ExportFormat.KMZ -> Pair(
                            format.displayName,
                            stringResource(id = R.string.export_kmz_desc)
                        )
                        ExportFormat.GEOJSON -> Pair(
                            format.displayName,
                            stringResource(id = R.string.export_geojson_desc)
                        )
                        ExportFormat.CSV -> Pair(
                            format.displayName,
                            stringResource(id = R.string.export_csv_desc)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isExporting) { selectedFormat = format },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedFormat = format },
                                enabled = !isExporting
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.exporting_in_progress),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedFormat) },
                enabled = !isExporting
            ) {
                Text(text = stringResource(id = R.string.export_action_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text(text = stringResource(id = R.string.cancel_button))
            }
        }
    )
}
