package com.zirosaur.ziromaps.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRecordingManager @Inject constructor() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _activeTrackId = MutableStateFlow<String?>(null)
    val activeTrackId: StateFlow<String?> = _activeTrackId.asStateFlow()

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _recordedDistanceM = MutableStateFlow(0.0)
    val recordedDistanceM: StateFlow<Double> = _recordedDistanceM.asStateFlow()

    private val _recordedPointsCount = MutableStateFlow(0)
    val recordedPointsCount: StateFlow<Int> = _recordedPointsCount.asStateFlow()

    fun onTrackStarted(trackId: String, projectId: String) {
        _isRecording.value = true
        _activeTrackId.value = trackId
        _activeProjectId.value = projectId
        _recordedDistanceM.value = 0.0
        _recordedPointsCount.value = 0
    }

    fun onPointRecorded(distanceM: Double, pointsCount: Int) {
        _recordedDistanceM.value = distanceM
        _recordedPointsCount.value = pointsCount
    }

    fun onTrackStopped() {
        _isRecording.value = false
        _activeTrackId.value = null
        _activeProjectId.value = null
        _recordedDistanceM.value = 0.0
        _recordedPointsCount.value = 0
    }
}
