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
        alertType: String,
        message: String? = null,
        contactName: String? = null,
        contactPhone: String? = null
    ) {
        Log.d("NETWORK", "Calling backend alert API: $logTag")
        Log.e(
            "NETWORK_DEBUG",
            "HITTING URL: $BASE_URL/api/alert | type=$alertType"
        )

        val json = JSONObject().apply {
            put("userId", userId)
            put("timestamp", System.currentTimeMillis())
            put("alertType", alertType)
            if (!message.isNullOrBlank()) {
                put("message", message)
            }
            if (!contactName.isNullOrBlank()) {
                put("contactName", contactName)
            }
            if (!contactPhone.isNullOrBlank()) {
                put("contactPhone", contactPhone)
            }
        }
        Log.d("NETWORK_DEBUG", "Alert payload: ${json.toString()}")

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
                    val body = it.body?.string()
                    Log.d("NETWORK", "$logTag response code: ${it.code}")
                    if (!body.isNullOrBlank()) {
                        Log.d("NETWORK_DEBUG", "$logTag response body: $body")
                    }
                }
            }
        })
    }

    fun sendWatchRemovedAlert(userId: String, message: String? = null) =
        sendAlert(userId, "watch-removed", "WATCH_REMOVED", message = message)

    fun sendFallDetectedAlert(userId: String, message: String? = null) =
        sendAlert(userId, "fall-detected", "FALL_DETECTED", message = message)

    fun panicButtonPressed(userId: String, message: String? = null) =
        sendAlert(userId, "panic-button", "PANIC", message = message)

    fun sendDangerousHeartRateAlert(userId: String, message: String? = null) =
        sendAlert(userId, "dangerous-heart-rate", "DANGEROUS_HR", message = message)

    fun sendEmergencyCallAlert(
        userId: String,
        contactName: String,
        contactPhone: String,
        message: String? = null
    ) = sendAlert(
        userId = userId,
        logTag = "emergency-call",
        alertType = "EMERGENCY_CALL",
        message = message,
        contactName = contactName,
        contactPhone = contactPhone
    )
}
