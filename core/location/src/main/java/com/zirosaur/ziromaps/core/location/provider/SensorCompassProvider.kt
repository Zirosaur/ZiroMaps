package com.zirosaur.ziromaps.core.location.provider

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.zirosaur.ziromaps.core.location.model.CompassData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorCompassProvider @Inject constructor(
    private val sensorManager: SensorManager
) : CompassProvider {

    override fun getCompassUpdates(): Flow<CompassData> = callbackFlow {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVectorSensor != null) {
            val listener = object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val orientationAngles = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val azimuthRad = orientationAngles[0]
                        val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
                        trySend(CompassData(azimuthDeg))
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            // Fallback: Accelerometer + Magnetometer
            val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (accelSensor == null || magnetSensor == null) {
                // If device lacks sensors, emit stationary True North (0°)
                trySend(CompassData(0f))
                awaitClose {}
                return@callbackFlow
            }

            val listener = object : SensorEventListener {
                private var lastAccel: FloatArray? = null
                private var lastMagnet: FloatArray? = null
                private val rotationMatrix = FloatArray(9)
                private val inclinationMatrix = FloatArray(9)
                private val orientationAngles = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> lastAccel = event.values.clone()
                        Sensor.TYPE_MAGNETIC_FIELD -> lastMagnet = event.values.clone()
                    }

                    val accel = lastAccel
                    val magnet = lastMagnet
                    if (accel != null && magnet != null) {
                        val success = SensorManager.getRotationMatrix(
                            rotationMatrix,
                            inclinationMatrix,
                            accel,
                            magnet
                        )
                        if (success) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val azimuthRad = orientationAngles[0]
                            val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
                            trySend(CompassData(azimuthDeg))
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
    }
}
