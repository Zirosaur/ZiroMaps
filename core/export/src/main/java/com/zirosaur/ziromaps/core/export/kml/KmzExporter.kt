package com.zirosaur.ziromaps.core.export.kml

import com.zirosaur.ziromaps.core.export.model.ExportDataset
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KmzExporter @Inject constructor() {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportToKmz(dataset: ExportDataset, destinationFile: File): File {
        destinationFile.parentFile?.mkdirs()

        val kmlContent = buildKml(dataset)

        FileOutputStream(destinationFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. Write doc.kml
                val kmlEntry = ZipEntry("doc.kml")
                zos.putNextEntry(kmlEntry)
                zos.write(kmlContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Write attached photos into images/ folder
                val includedFiles = mutableSetOf<String>()
                for (wp in dataset.waypoints) {
                    val filePath = wp.photoFilePath ?: continue
                    val file = File(filePath)
                    if (file.exists() && file.isFile) {
                        val entryName = "images/${file.name}"
                        if (includedFiles.add(entryName)) {
                            val photoEntry = ZipEntry(entryName)
                            zos.putNextEntry(photoEntry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }

                zos.flush()
            }
        }

        return destinationFile
    }

    fun buildKml(dataset: ExportDataset): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append("\n")
        sb.append("<Document>\n")
        sb.append("  <name><![CDATA[").append(dataset.projectName).append("]]></name>\n")

        // Styles
        sb.append("  <Style id=\"waypointPin\">\n")
        sb.append("    <IconStyle>\n")
        sb.append("      <color>ff0000ff</color>\n")
        sb.append("      <scale>1.1</scale>\n")
        sb.append("      <Icon><href>http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png</href></Icon>\n")
        sb.append("    </IconStyle>\n")
        sb.append("  </Style>\n")

        sb.append("  <Style id=\"photoPin\">\n")
        sb.append("    <IconStyle>\n")
        sb.append("      <color>ffff00ff</color>\n")
        sb.append("      <scale>1.2</scale>\n")
        sb.append("      <Icon><href>http://maps.google.com/mapfiles/kml/shapes/camera.png</href></Icon>\n")
        sb.append("    </IconStyle>\n")
        sb.append("  </Style>\n")

        sb.append("  <Style id=\"trackLine\">\n")
        sb.append("    <LineStyle>\n")
        sb.append("      <color>ff00a5ff</color>\n")
        sb.append("      <width>4</width>\n")
        sb.append("    </LineStyle>\n")
        sb.append("  </Style>\n")

        // 1. Waypoints Folder
        if (dataset.waypoints.isNotEmpty()) {
            sb.append("  <Folder>\n")
            sb.append("    <name>Waypoints</name>\n")

            for (wp in dataset.waypoints) {
                sb.append("    <Placemark>\n")
                sb.append("      <name><![CDATA[").append(wp.title).append("]]></name>\n")
                sb.append("      <styleUrl>#").append(if (wp.hasPhoto) "photoPin" else "waypointPin").append("</styleUrl>\n")

                sb.append("      <description><![CDATA[\n")
                if (wp.note.isNotBlank()) {
                    sb.append("        <p><b>Catatan:</b> ").append(wp.note).append("</p>\n")
                }
                if (wp.accuracyM != null) {
                    sb.append("        <p><b>Akurasi:</b> ±").append(String.format(Locale.US, "%.1f", wp.accuracyM)).append(" m</p>\n")
                }
                if (wp.photoAzimuthDeg != null) {
                    sb.append("        <p><b>Azimuth:</b> ").append(String.format(Locale.US, "%.0f°", wp.photoAzimuthDeg)).append("</p>\n")
                }
                sb.append("        <p><b>Waktu:</b> ").append(isoDateFormat.format(Date(wp.createdAtEpochMs))).append("</p>\n")
                if (wp.photoFilePath != null) {
                    val fileName = File(wp.photoFilePath).name
                    sb.append("        <p><img src=\"images/").append(fileName).append("\" width=\"320\" /></p>\n")
                }
                sb.append("      ]]></description>\n")

                val alt = wp.altitudeM ?: 0.0
                sb.append("      <Point>\n")
                sb.append("        <coordinates>")
                sb.append(String.format(Locale.US, "%.6f,%.6f,%.1f", wp.longitude, wp.latitude, alt))
                sb.append("</coordinates>\n")
                sb.append("      </Point>\n")
                sb.append("    </Placemark>\n")
            }

            sb.append("  </Folder>\n")
        }

        // 2. Tracks Folder
        if (dataset.tracks.isNotEmpty()) {
            sb.append("  <Folder>\n")
            sb.append("    <name>Tracks</name>\n")

            for (track in dataset.tracks) {
                if (track.points.isEmpty()) continue

                sb.append("    <Placemark>\n")
                sb.append("      <name><![CDATA[").append(track.name).append("]]></name>\n")
                sb.append("      <styleUrl>#trackLine</styleUrl>\n")
                sb.append("      <description><![CDATA[\n")
                sb.append("        <p><b>Total Jarak:</b> ").append(String.format(Locale.US, "%.2f km", track.totalDistanceM / 1000.0)).append("</p>\n")
                sb.append("        <p><b>Titik:</b> ").append(track.points.size).append("</p>\n")
                sb.append("        <p><b>Mulai:</b> ").append(isoDateFormat.format(Date(track.startedAtEpochMs))).append("</p>\n")
                if (track.endedAtEpochMs != null) {
                    sb.append("        <p><b>Selesai:</b> ").append(isoDateFormat.format(Date(track.endedAtEpochMs))).append("</p>\n")
                }
                sb.append("      ]]></description>\n")

                sb.append("      <LineString>\n")
                sb.append("        <extrude>0</extrude>\n")
                sb.append("        <tessellate>1</tessellate>\n")
                sb.append("        <altitudeMode>clampToGround</altitudeMode>\n")
                sb.append("        <coordinates>\n          ")

                val coordString = track.points.joinToString(" ") { pt ->
                    val alt = pt.altitudeM ?: 0.0
                    String.format(Locale.US, "%.6f,%.6f,%.1f", pt.longitude, pt.latitude, alt)
                }
                sb.append(coordString).append("\n")

                sb.append("        </coordinates>\n")
                sb.append("      </LineString>\n")
                sb.append("    </Placemark>\n")
            }

            sb.append("  </Folder>\n")
        }

        sb.append("</Document>\n")
        sb.append("</kml>")
        return sb.toString()
    }
}
