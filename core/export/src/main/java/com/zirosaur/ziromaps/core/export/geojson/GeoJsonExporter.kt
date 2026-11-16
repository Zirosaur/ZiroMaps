package com.zirosaur.ziromaps.core.export.geojson

import com.zirosaur.ziromaps.core.export.model.ExportDataset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoJsonExporter @Inject constructor() {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportToGeoJson(dataset: ExportDataset, destinationFile: File): File {
        val root = JSONObject()
        root.put("type", "FeatureCollection")
        root.put("name", dataset.projectName)

        val features = JSONArray()

        // 1. Waypoints
        for (wp in dataset.waypoints) {
            val feature = JSONObject()
            feature.put("type", "Feature")

            // Geometry (RFC 7946: [lon, lat, (alt)])
            val geometry = JSONObject()
            geometry.put("type", "Point")
            val coords = JSONArray()
            coords.put(wp.longitude)
            coords.put(wp.latitude)
            if (wp.altitudeM != null) {
                coords.put(wp.altitudeM)
            }
            geometry.put("coordinates", coords)
            feature.put("geometry", geometry)

            // Properties
            val props = JSONObject()
            props.put("id", wp.id)
            props.put("name", wp.title)
            props.put("note", wp.note)
            props.put("hasPhoto", wp.hasPhoto)
            if (wp.accuracyM != null) {
                props.put("accuracyM", wp.accuracyM.toDouble())
            }
            if (wp.photoAzimuthDeg != null) {
                props.put("azimuthDeg", wp.photoAzimuthDeg.toDouble())
            }
            if (!wp.photoFilePath.isNullOrBlank()) {
                props.put("photoFileName", File(wp.photoFilePath).name)
            }
            props.put("createdAt", isoDateFormat.format(Date(wp.createdAtEpochMs)))

            feature.put("properties", props)
            features.put(feature)
        }

        // 2. Tracks
        for (track in dataset.tracks) {
            if (track.points.isEmpty()) continue

            val feature = JSONObject()
            feature.put("type", "Feature")

            // Geometry: LineString
            val geometry = JSONObject()
            geometry.put("type", "LineString")
            val coords = JSONArray()
            for (pt in track.points) {
                val ptCoord = JSONArray()
                ptCoord.put(pt.longitude)
                ptCoord.put(pt.latitude)
                if (pt.altitudeM != null) {
                    ptCoord.put(pt.altitudeM)
                }
                coords.put(ptCoord)
            }
            geometry.put("coordinates", coords)
            feature.put("geometry", geometry)

            // Properties
            val props = JSONObject()
            props.put("id", track.id)
            props.put("name", track.name)
            props.put("totalDistanceM", track.totalDistanceM)
            props.put("startedAt", isoDateFormat.format(Date(track.startedAtEpochMs)))
            if (track.endedAtEpochMs != null) {
                props.put("endedAt", isoDateFormat.format(Date(track.endedAtEpochMs)))
            }
            props.put("pointCount", track.points.size)

            feature.put("properties", props)
            features.put(feature)
        }

        root.put("features", features)

        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            out.flush()
        }

        return destinationFile
    }
}
