package com.zirosaur.ziromaps.core.export.csv

import com.zirosaur.ziromaps.core.export.model.ExportDataset
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor() {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportToCsv(dataset: ExportDataset, destinationFile: File): File {
        destinationFile.parentFile?.mkdirs()

        FileOutputStream(destinationFile).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                // Header (RFC 4180)
                writer.write("ID,Name,Type,Latitude,Longitude,Altitude_m,Accuracy_m,Azimuth_deg,Note,Has_Photo,Photo_File,Created_At\r\n")

                // Waypoints
                for (wp in dataset.waypoints) {
                    val type = if (wp.hasPhoto) "PHOTO" else "WAYPOINT"
                    val altStr = wp.altitudeM?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                    val accStr = wp.accuracyM?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                    val azStr = wp.photoAzimuthDeg?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                    val photoFile = wp.photoFilePath?.let { File(it).name } ?: ""
                    val createdAt = isoDateFormat.format(Date(wp.createdAtEpochMs))

                    val row = listOf(
                        escapeCsv(wp.id),
                        escapeCsv(wp.title),
                        type,
                        String.format(Locale.US, "%.6f", wp.latitude),
                        String.format(Locale.US, "%.6f", wp.longitude),
                        altStr,
                        accStr,
                        azStr,
                        escapeCsv(wp.note),
                        wp.hasPhoto.toString(),
                        escapeCsv(photoFile),
                        createdAt
                    ).joinToString(",")

                    writer.write(row + "\r\n")
                }

                // Track Points
                for (track in dataset.tracks) {
                    for ((index, pt) in track.points.withIndex()) {
                        val altStr = pt.altitudeM?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                        val accStr = pt.accuracyM?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                        val timeStr = isoDateFormat.format(Date(pt.recordedAtEpochMs))

                        val row = listOf(
                            escapeCsv("${track.id}_$index"),
                            escapeCsv("${track.name} [pt $index]"),
                            "TRACK_POINT",
                            String.format(Locale.US, "%.6f", pt.latitude),
                            String.format(Locale.US, "%.6f", pt.longitude),
                            altStr,
                            accStr,
                            "",
                            escapeCsv("Track: ${track.name}"),
                            "false",
                            "",
                            timeStr
                        ).joinToString(",")

                        writer.write(row + "\r\n")
                    }
                }

                writer.flush()
            }
        }

        return destinationFile
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
