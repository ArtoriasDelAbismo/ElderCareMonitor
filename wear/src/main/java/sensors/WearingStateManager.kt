package com.example.eldercaremonitor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class WearingStateManager(
    context: Context,
    private val onWearingStateChanged: (Boolean) -> Unit
) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // The off-body detection sensor
    private val offBodySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

    private var lastState: Boolean? = null

    private val sensorListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return

            // event.values[0] = 0.0 → ON BODY
            // event.values[0] = 1.0 → OFF BODY
            val raw = event.values[0].toInt()
            val isWearing = raw == 0

            if (lastState == null || lastState != isWearing) {
                lastState = isWearing
                onWearingStateChanged(isWearing)
                Log.d("WEARING", "User wearing watch: $isWearing (raw=$raw)")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // You can log accuracy changes if needed
        }
    }

    fun start() {
        if (offBodySensor == null) {
            Log.e("WEARING", "No off-body sensor available on this device.")
            return
        }

        sensorManager.registerListener(
            sensorListener,
            offBodySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        Log.d("WEARING", "Wearing detection started.")
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
        Log.d("WEARING", "Wearing detection stopped.")
    }
}
