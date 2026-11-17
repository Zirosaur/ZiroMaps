package com.zirosaur.ziromaps.data.preferences

import android.content.Context
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkConfig
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkPosition
import com.zirosaur.ziromaps.core.camera.watermark.WatermarkTextSize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StampSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ziromaps_stamp_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_POSITION = "stamp_position"
        private const val KEY_TEXT_SIZE = "stamp_text_size"
        private const val KEY_SHOW_LAT_LON = "stamp_show_lat_lon"
        private const val KEY_SHOW_UTM = "stamp_show_utm"
        private const val KEY_SHOW_AZIMUTH = "stamp_show_azimuth"
        private const val KEY_SHOW_ALTITUDE = "stamp_show_altitude"
        private const val KEY_SHOW_TIMESTAMP = "stamp_show_timestamp"
        private const val KEY_SHOW_MAP_NAME = "stamp_show_map_name"
        private const val KEY_SHOW_NOTE = "stamp_show_note"
        private const val KEY_LAST_PHOTO_PATH = "stamp_last_photo_path"
    }

    fun loadConfig(): WatermarkConfig {
        val posStr = prefs.getString(KEY_POSITION, WatermarkPosition.BOTTOM_LEFT.name)
        val pos = try {
            WatermarkPosition.valueOf(posStr ?: WatermarkPosition.BOTTOM_LEFT.name)
        } catch (_: Exception) {
            WatermarkPosition.BOTTOM_LEFT
        }

        val sizeStr = prefs.getString(KEY_TEXT_SIZE, WatermarkTextSize.MEDIUM.name)
        val size = try {
            WatermarkTextSize.valueOf(sizeStr ?: WatermarkTextSize.MEDIUM.name)
        } catch (_: Exception) {
            WatermarkTextSize.MEDIUM
        }

        return WatermarkConfig(
            position = pos,
            textSize = size,
            showLatLon = prefs.getBoolean(KEY_SHOW_LAT_LON, true),
            showUtm = prefs.getBoolean(KEY_SHOW_UTM, true),
            showAzimuth = prefs.getBoolean(KEY_SHOW_AZIMUTH, true),
            showAltitude = prefs.getBoolean(KEY_SHOW_ALTITUDE, true),
            showTimestamp = prefs.getBoolean(KEY_SHOW_TIMESTAMP, true),
            showMapName = prefs.getBoolean(KEY_SHOW_MAP_NAME, true),
            showNote = prefs.getBoolean(KEY_SHOW_NOTE, true)
        )
    }

    fun saveConfig(config: WatermarkConfig) {
        prefs.edit()
            .putString(KEY_POSITION, config.position.name)
            .putString(KEY_TEXT_SIZE, config.textSize.name)
            .putBoolean(KEY_SHOW_LAT_LON, config.showLatLon)
            .putBoolean(KEY_SHOW_UTM, config.showUtm)
            .putBoolean(KEY_SHOW_AZIMUTH, config.showAzimuth)
            .putBoolean(KEY_SHOW_ALTITUDE, config.showAltitude)
            .putBoolean(KEY_SHOW_TIMESTAMP, config.showTimestamp)
            .putBoolean(KEY_SHOW_MAP_NAME, config.showMapName)
            .putBoolean(KEY_SHOW_NOTE, config.showNote)
            .apply()
    }

    fun getLastPhotoPath(): String? {
        return prefs.getString(KEY_LAST_PHOTO_PATH, null)
    }

    fun setLastPhotoPath(path: String) {
        prefs.edit().putString(KEY_LAST_PHOTO_PATH, path).apply()
    }
}
