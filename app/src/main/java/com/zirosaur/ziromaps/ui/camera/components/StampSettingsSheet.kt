package com.zirosaur.ziromaps.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkConfig
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkPosition
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkTextSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampSettingsSheet(
    config: WatermarkConfig,
    quickNote: String,
    onQuickNoteChange: (String) -> Unit,
    onConfigChange: (WatermarkConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Pengaturan Stamp Lapangan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sesuaikan informasi yang dicetak pada foto lapangan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Note Input Field
            Text(
                text = "Catatan Cepat (Langsung Dicetak)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = quickNote,
                onValueChange = onQuickNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: Patok Batas B-14 kondisi baik") },
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Watermark Position Selector
            Text(
                text = "Posisi Watermark",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    WatermarkPosition.BOTTOM_LEFT to "Kiri Bawah",
                    WatermarkPosition.BOTTOM_RIGHT to "Kanan Bawah",
                    WatermarkPosition.TOP_LEFT to "Kiri Atas"
                ).forEach { (pos, label) ->
                    val isSelected = config.position == pos
                    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable { onConfigChange(config.copy(position = pos)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Watermark Text Size Selector
            Text(
                text = "Ukuran Teks Stamp",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    WatermarkTextSize.SMALL to "Kecil (0.8x)",
                    WatermarkTextSize.MEDIUM to "Sedang (1.0x)",
                    WatermarkTextSize.LARGE to "Besar (1.25x)"
                ).forEach { (size, label) ->
                    val isSelected = config.textSize == size
                    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable { onConfigChange(config.copy(textSize = size)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Switches for Fields
            Text(
                text = "Bidang yang Dicetak",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FieldToggleRow(
                label = "Koordinat Geografis (Lat/Lon)",
                checked = config.showLatLon,
                onCheckedChange = { onConfigChange(config.copy(showLatLon = it)) }
            )
            FieldToggleRow(
                label = "Koordinat Proyeksi UTM (WGS84)",
                checked = config.showUtm,
                onCheckedChange = { onConfigChange(config.copy(showUtm = it)) }
            )
            FieldToggleRow(
                label = "Arah Kompas / Azimuth",
                checked = config.showAzimuth,
                onCheckedChange = { onConfigChange(config.copy(showAzimuth = it)) }
            )
            FieldToggleRow(
                label = "Elevasi / Ketinggian (Altitude)",
                checked = config.showAltitude,
                onCheckedChange = { onConfigChange(config.copy(showAltitude = it)) }
            )
            FieldToggleRow(
                label = "Waktu & Tanggal",
                checked = config.showTimestamp,
                onCheckedChange = { onConfigChange(config.copy(showTimestamp = it)) }
            )
            FieldToggleRow(
                label = "Nama Proyek Peta",
                checked = config.showMapName,
                onCheckedChange = { onConfigChange(config.copy(showMapName = it)) }
            )
            FieldToggleRow(
                label = "Catatan Cepat",
                checked = config.showNote,
                onCheckedChange = { onConfigChange(config.copy(showNote = it)) }
            )
        }
    }
}

@Composable
private fun FieldToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
