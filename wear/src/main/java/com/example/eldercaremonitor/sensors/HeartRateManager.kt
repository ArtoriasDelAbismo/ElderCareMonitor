package com.example.eldercaremonitor.sensors

import android.util.Log
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.data.*

class HeartRateManager(
    private val measureClient: MeasureClient,
    private val onHeartRateChanged: (Int) -> Unit,
) {

    private var zeroCount = 0            // counts consecutive 0 BPM readings
    private val zeroLimit = 3           // ignore isolated 0 BPM unless repeated
    private var isStarted = false

    private val callback = object : MeasureCallback {
        override fun onDataReceived(data: DataPointContainer) {
            val hrPoint = data.getData(DataType.HEART_RATE_BPM).firstOrNull() ?: return
            val bpm = hrPoint.value.toInt()

            // -------------------------
            // ZERO BPM FILTERING
            // -------------------------
            if (bpm == 0) {
                zeroCount++
                Log.d("HEART", "Zero detected: $zeroCount")

                if (zeroCount < zeroLimit) {
                    // Ignore initial zeros
                    return
                }
                // If zeroLimit reached, accept zero as a valid reading
            } else {
                zeroCount = 0 // reset when valid BPM appears
            }

            // -------------------------
            // Send BPM to UI
            // -------------------------
            onHeartRateChanged(bpm)

            Log.d("HEART", "HR = $bpm")
        }

        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            Log.d("HEART", "Availability changed: $dataType, $availability")
        }
    }

    fun start() {
        if(isStarted) return
        isStarted = true
        zeroCount = 0
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
        Log.d("HEART", "HeartRateManager started")
    }

    fun stop() {
        if(!isStarted) return
        isStarted = false
        measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
        Log.d("HEART", "HeartRateManager stopped")
    }
}
