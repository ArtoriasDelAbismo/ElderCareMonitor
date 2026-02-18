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
    private var lastImpactTime = 0L
    private var lastStillnessTime = 0L
    private var previousAcceleration = 9.8f
    private var freeFallStartTime = 0L
    private var lastFreeFallTime = 0L

    // ----- Thresholds -----
    private val impactThreshold = 32f               // strong impact
    private val requiredStillnessTime = 2000L       // 2s still after impact
    private val stillnessDeltaThreshold = 0.8f      // near-zero movement
    private val movementDeltaThreshold = 1.5f       // reset stillness on movement
    private val impactStillnessWindow = 4000L       // stillness must follow soon after impact
    private val freeFallThreshold = 2.0f            // near-free-fall magnitude
    private val freeFallMinDuration = 200L          // sustain free-fall briefly
    private val freeFallToImpactWindow = 800L       // impact must follow free-fall

    // Fall cooldown to avoid spam detections
    private val fallCooldown = 5000L                // 5 seconds
    private var lastFallTriggeredTime = 0L

    fun start() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME // faster data → more reliable
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        handleAccelerometer(event)
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val (x, y, z) = event.values
        val acceleration = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        // ------- FREE-FALL DETECTION -------
        if (acceleration < freeFallThreshold) {
            if (freeFallStartTime == 0L) {
                freeFallStartTime = now
            } else if (now - freeFallStartTime >= freeFallMinDuration) {
                lastFreeFallTime = now
            }
        } else {
            freeFallStartTime = 0L
        }

        // ------- STILLNESS ANALYSIS -------
        val delta = abs(acceleration - previousAcceleration)
        previousAcceleration = acceleration

        if (delta < stillnessDeltaThreshold) {
            if (lastStillnessTime == 0L) lastStillnessTime = now
        } else if (delta > movementDeltaThreshold) {
            lastStillnessTime = 0L
        }

        // ------- IMPACT DETECTION -------
        if (acceleration > impactThreshold) {
            val freeFallRecent = now - lastFreeFallTime <= freeFallToImpactWindow
            if (freeFallRecent) {
                lastImpactTime = now
                Log.d("IMPACT", "Impact detected: $acceleration")
            } else {
                Log.d("IMPACT", "Impact ignored (no recent free-fall): $acceleration")
            }
        }

        // ------- COOLDOWN TO PREVENT SPAM -------
        if (now - lastFallTriggeredTime < fallCooldown) return

        // If impact is too old, clear it so stillness doesn't trip later.
        if (lastImpactTime > 0 && now - lastImpactTime > impactStillnessWindow) {
            lastImpactTime = 0L
            lastStillnessTime = 0L
        }

        // ------- FALL DECISION LOGIC -------
        val stillnessDuration =
            if (lastStillnessTime > 0) now - lastStillnessTime else 0

        val isFall =
            lastImpactTime > 0 &&
                    stillnessDuration > requiredStillnessTime

        if (isFall) {
            Log.d("FALL", "FALL DETECTED — Triggering callback")

            lastFallTriggeredTime = now
            onFallDetected()

            // Reset states
            lastImpactTime = 0L
            lastStillnessTime = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun reset() {
        lastImpactTime = 0L
        lastStillnessTime = 0L
        previousAcceleration = 9.8f
        freeFallStartTime = 0L
        lastFreeFallTime = 0L
        Log.d("FALL", "Fall reset")
    }
}
