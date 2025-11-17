

package com.example.eldercaremonitor.sensors

import android.util.Log
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.data.*

class HeartRateMonitor(
    private val measureClient: MeasureClient,
    private val onHeartRateChanged: (Int) -> Unit,
    private val onDangerousHeartRate: (Int) -> Unit
) {

    private val callback = object : MeasureCallback {
        override fun onDataReceived(data: DataPointContainer) {
            val hr = data.getData(DataType.HEART_RATE_BPM).firstOrNull()
            if (hr != null) {
                val bpm = hr.value.toInt()
                onHeartRateChanged(bpm)

                // Check for dangerous bpm
                if(bpm > HIGH_BPM_THRESHOLD || bpm < LOW_BPM_THRESHOLD){
                    onDangerousHeartRate(bpm)
                }

                Log.d("HEART", "HR = $bpm")
            }
        }

        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            Log.d("AVAIL", "Availability changed: $dataType, $availability")
        }
    }

    fun start() {
        measureClient.registerMeasureCallback(
            DataType.HEART_RATE_BPM,
            callback
        )
    }

    fun stop() {
        measureClient.unregisterMeasureCallbackAsync(
            DataType.HEART_RATE_BPM,
            callback
        )
    }

    companion object {
        const val HIGH_BPM_THRESHOLD = 120
        const val LOW_BPM_THRESHOLD = 45
    }
}
