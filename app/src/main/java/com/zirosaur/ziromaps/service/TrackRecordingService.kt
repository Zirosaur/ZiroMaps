package com.zirosaur.ziromaps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zirosaur.ziromaps.MainActivity
import com.zirosaur.ziromaps.R
import com.zirosaur.ziromaps.core.database.dao.TrackDao
import com.zirosaur.ziromaps.core.database.entity.TrackEntity
import com.zirosaur.ziromaps.core.database.entity.TrackPointEntity
import com.zirosaur.ziromaps.core.location.model.UserLocation
import com.zirosaur.ziromaps.core.location.provider.LocationProvider
import com.zirosaur.ziromaps.util.GeoDistance
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class TrackRecordingService : Service() {

    @Inject
    lateinit var trackDao: TrackDao

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var recordingManager: TrackRecordingManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null

    private var currentTrackId: String? = null
    private var lastRecordedLocation: UserLocation? = null
    private var totalDistanceMeters = 0.0
    private var totalPointsCount = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
                val trackName = intent.getStringExtra(EXTRA_TRACK_NAME) ?: "Track ${System.currentTimeMillis()}"
                if (projectId.isNotBlank() && currentTrackId == null) {
                    startRecording(projectId, trackName)
                }
            }
            ACTION_STOP -> {
                stopRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(projectId: String, trackName: String) {
        val trackId = UUID.randomUUID().toString()
        currentTrackId = trackId
        totalDistanceMeters = 0.0
        totalPointsCount = 0
        lastRecordedLocation = null

        val initialNotification = buildNotification(totalDistanceMeters, totalPointsCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        recordingManager.onTrackStarted(trackId, projectId)

        recordingJob = serviceScope.launch {
            val track = TrackEntity(
                id = trackId,
                projectId = projectId,
                name = trackName,
                startedAtEpochMs = System.currentTimeMillis(),
                endedAtEpochMs = null,
                totalDistanceM = 0.0
            )
            trackDao.insertTrack(track)

            locationProvider.getLocationUpdates(intervalMs = 2000L).collect { location ->
                val prev = lastRecordedLocation
                if (prev != null) {
                    val deltaMeters = GeoDistance.haversineMeters(
                        prev.latitude, prev.longitude,
                        location.latitude, location.longitude
                    )
                    // Discard jitter (< 1.5 m)
                    if (deltaMeters >= 1.5) {
                        totalDistanceMeters += deltaMeters
                    }
                }
                lastRecordedLocation = location
                totalPointsCount++

                val point = TrackPointEntity(
                    trackId = trackId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitudeM = location.altitudeM,
                    accuracyM = location.accuracyM,
                    recordedAtEpochMs = location.timestampEpochMs
                )
                trackDao.insertTrackPoint(point)
                trackDao.updateTrack(track.copy(totalDistanceM = totalDistanceMeters))

                recordingManager.onPointRecorded(totalDistanceMeters, totalPointsCount)
                updateNotification(totalDistanceMeters, totalPointsCount)
            }
        }
    }

    private fun stopRecording() {
        val trackId = currentTrackId
        recordingJob?.cancel()
        recordingJob = null

        if (trackId != null) {
            serviceScope.launch {
                val track = trackDao.getTrackByIdSync(trackId)
                if (track != null) {
                    trackDao.updateTrack(
                        track.copy(
                            endedAtEpochMs = System.currentTimeMillis(),
                            totalDistanceM = totalDistanceMeters
                        )
                    )
                }
            }
        }

        recordingManager.onTrackStopped()
        currentTrackId = null
        lastRecordedLocation = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(distanceM: Double, points: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceKm = distanceM / 1000.0
        val content = getString(R.string.track_status_format, distanceKm, points)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_track_title))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_track)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(distanceM: Double, points: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(distanceM, points))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.track_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.track_notification_desc)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "track_recording_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.zirosaur.ziromaps.action.START_TRACK"
        const val ACTION_STOP = "com.zirosaur.ziromaps.action.STOP_TRACK"

        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_TRACK_NAME = "extra_track_name"

        fun start(context: Context, projectId: String, trackName: String) {
            val intent = Intent(context, TrackRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_TRACK_NAME, trackName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
