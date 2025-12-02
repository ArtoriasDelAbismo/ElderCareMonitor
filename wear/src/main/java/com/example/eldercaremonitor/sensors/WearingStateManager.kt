package com.example.eldercaremonitor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper

class WearingStateManager(
    context: Context,
    private val onWorn: () -> Unit,
    private val onRemoved: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val handler = Handler(Looper.getMainLooper())
    private var lastHeartRateTime: Long? = null

    private var isWorn = false
    private val removalTimeout = 18_000L // 18 seconds
    private val startupDelay = 5_000L    // Wait 5 seconds before assuming not worn

    private val removalChecker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()

            val lastTime = lastHeartRateTime
            if (lastTime != null) {
                // Normal operation: no HR reading for removalTimeout → removed
                if (now - lastTime >= removalTimeout && isWorn) {
                    isWorn = false
                    onRemoved()
                }
            } else {
                // During startup, wait a few seconds before triggering "not worn"
                if (!isWorn && now >= startupDelay) {
                    isWorn = false
                    onRemoved()
                }
            }

            handler.postDelayed(this, 500) // check every 0.5 sec
        }
    }

    fun start() {
        heartRateSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Record start time
        val startTime = System.currentTimeMillis()
        handler.post(object : Runnable {
            override fun run() {
                if (lastHeartRateTime == null) {
                    lastHeartRateTime = startTime
                }
                handler.postDelayed(this, 500)
            }
        })

        handler.postDelayed(removalChecker, startupDelay)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val hr = event.values[0]

            if (hr > 0) {
                lastHeartRateTime = System.currentTimeMillis()

                if (!isWorn) {
                    isWorn = true
                    onWorn()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
