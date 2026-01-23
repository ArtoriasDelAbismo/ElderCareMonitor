package com.example.eldercaremonitor.data.location

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WatchLocationHelper(context: Context) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    fun getLastKnownLocationJson(timeoutMs: Long = 1200): JSONObject? {
        return try {
            val location = Tasks.await(fused.lastLocation, timeoutMs, TimeUnit.MILLISECONDS)
                ?: return null

            JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
            }
        } catch (e: Exception) {null}
    }
}