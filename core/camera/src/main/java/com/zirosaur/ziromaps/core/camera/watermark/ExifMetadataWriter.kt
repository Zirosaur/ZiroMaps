package com.zirosaur.ziromaps.core.camera.watermark

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

interface ExifMetadataWriter {
    fun writeExifMetadata(file: File, data: WatermarkData)
    fun writeExifMetadata(exif: ExifInterface, data: WatermarkData)
}

@Singleton
class ExifMetadataWriterImpl @Inject constructor() : ExifMetadataWriter {

    override fun writeExifMetadata(file: File, data: WatermarkData) {
        val exif = ExifInterface(file.absolutePath)
        writeExifMetadata(exif, data)
        exif.saveAttributes()
    }

    override fun writeExifMetadata(exif: ExifInterface, data: WatermarkData) {
        // 1. GPS Coordinates
        if (data.latitude != null && data.longitude != null) {
            val latDms = decimalToDmsRational(data.latitude)
            val lonDms = decimalToDmsRational(data.longitude)

            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latDms)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (data.latitude >= 0) "N" else "S")

            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonDms)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (data.longitude >= 0) "E" else "W")
        }

        // 2. GPS Altitude
        if (data.altitudeM != null) {
            val altAbs = Math.abs(data.altitudeM)
            val altRational = "${(altAbs * 1000).toLong()}/1000"
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, altRational)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (data.altitudeM >= 0) "0" else "1")
        }

        // 3. Direction / Azimuth
        if (data.azimuthDeg != null) {
            val normAzimuth = (data.azimuthDeg % 360f + 360f) % 360f
            val azRational = "${(normAzimuth * 100).toInt()}/100"
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, azRational)
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M") // Magnetic compass
        }

        // 4. Timestamps
        val dateUtc = Date(data.timestampEpochMs)
        val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
            timeZone = data.timeZone
        }
        val formattedDate = exifDateFormat.format(dateUtc)

        exif.setAttribute(ExifInterface.TAG_DATETIME, formattedDate)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, formattedDate)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, formattedDate)

        // Timezone Offset (e.g. +07:00)
        val offsetFormat = SimpleDateFormat("XXX", Locale.US).apply {
            timeZone = data.timeZone
        }
        val offsetString = offsetFormat.format(dateUtc)
        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offsetString)

        // GPS Date & Timestamp (UTC)
        val gpsDateFormat = SimpleDateFormat("yyyy:MM:dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val gpsTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, gpsDateFormat.format(dateUtc))
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, gpsTimeFormat.format(dateUtc))

        // 5. User Comment & Description
        val descParts = listOfNotNull(
            data.mapProjectName?.takeIf { it.isNotBlank() }?.let { "Peta: $it" },
            data.note?.takeIf { it.isNotBlank() }?.let { "Catatan: $it" },
            data.utmCoordinate?.takeIf { it.isNotBlank() }?.let { "UTM: $it" }
        )
        if (descParts.isNotEmpty()) {
            val comment = descParts.joinToString(" | ")
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, comment)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment)
        }
    }

    private fun decimalToDmsRational(coordinate: Double): String {
        val abs = Math.abs(coordinate)
        val deg = abs.toInt()
        val minDouble = (abs - deg) * 60.0
        val min = minDouble.toInt()
        val secDouble = (minDouble - min) * 60.0
        val secInt = (secDouble * 1000).toLong()
        return "$deg/1,$min/1,$secInt/1000"
    }
}
