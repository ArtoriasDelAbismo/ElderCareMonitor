package data.network

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import safety.SafetyEngine
import java.io.IOException
import java.util.concurrent.TimeUnit

class AlertService {

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    val BASE_URL = "https://forgiving-lucia-crudely.ngrok-free.dev"

    private fun sendAlert(
        userId: String,
        logTag: String,
        alertType: String
    ) {
        Log.d("NETWORK", "Calling backend alert API: $logTag")

        val json = JSONObject().apply {
            put("userId", userId)
            put("timestamp", System.currentTimeMillis())
            put("alertType", alertType)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/api/alert")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NETWORK", "Failed to send $logTag alert", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d("NETWORK", "$logTag response code: ${it.code}")
                }
            }
        })
    }

    fun sendWatchRemovedAlert(userId: String) =
        sendAlert(userId, "watch-removed", "WATCH_REMOVED")

    fun sendFallDetectedAlert(userId: String) =
        sendAlert(userId, "fall-detected", "FALL_DETECTED")

    fun panicButtonPressed(userId: String) =
        sendAlert(userId, "panic-button", "PANIC")

    fun sendDangerousHeartRateAlert(userId: String) =
        sendAlert(userId, "dangerous-heart-rate", "DANGEROUS_HR")
}