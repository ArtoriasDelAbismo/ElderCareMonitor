package com.example.eldercaremonitor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class FallDetectionManager(
    context: Context,
    private val onFallDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // ----- Fall Detection State -----
    private var lastFreeFallTime = 0L
    private var lastImpactTime = 0L
    private var lastStillnessTime = 0L
    private var previousAcceleration = 9.8f  // baseline

    // ----- Thresholds -----
    private val freeFallThreshold = 2.0f                 // ~0g
    private val impactThreshold = 30f                    // strong impact
    private val maxDelayBetweenFallAndImpact = 1500L     // 1.5s
    private val requiredStillnessTime = 2000L            // 2s still after impact
    private val stillnessDeltaThreshold = 0.8f           // movement < 0.8 = still
    private val movementDeltaThreshold = 1.5f            // movement > 1.5 = motion

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        handleAccelerometer(event)
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z)
        Log.d("ACCEL", "Accel = $acceleration")

        val now = System.currentTimeMillis()

        // ---------- STILLNESS DETECTION ----------
        val delta = abs(acceleration - previousAcceleration)
        previousAcceleration = acceleration

        if (delta < stillnessDeltaThreshold) {
            // Not moving
            if (lastStillnessTime == 0L) lastStillnessTime = now
        } else if (delta > movementDeltaThreshold) {
            // Movement detected → reset stillness
            lastStillnessTime = 0L
        }

        // ---------- FREE FALL DETECTION ----------
        if (acceleration < freeFallThreshold) {
            lastFreeFallTime = now
            Log.d("FALL", "Free fall detected: $acceleration")
        }

        // ---------- IMPACT DETECTION ----------
        if (acceleration > impactThreshold) {
            lastImpactTime = now
            Log.d("IMPACT", "Impact detected: $acceleration")
        }

        // ---------- FALL DECISION LOGIC ----------
        val fallToImpactDelay = lastImpactTime - lastFreeFallTime
        val stillnessDuration = if (lastStillnessTime > 0) now - lastStillnessTime else 0

        val isFall =
            lastFreeFallTime > 0 &&
                    lastImpactTime > lastFreeFallTime &&
                    fallToImpactDelay in 0..maxDelayBetweenFallAndImpact &&
                    stillnessDuration > requiredStillnessTime

        if (isFall) {
            Log.d("FALL", "FALL DETECTED — Triggering callback")
            onFallDetected()

            // Reset all
            lastFreeFallTime = 0
            lastImpactTime = 0
            lastStillnessTime = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
