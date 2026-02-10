package data.network

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AlertService {

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    // TODO: Move to BuildConfig or config file so you don’t hardcode ngrok
    private val BASE_URL = "https://forgiving-lucia-crudely.ngrok-free.dev"

    // TODO: Replace with a real ID if you have one (device serial, installation id, etc.)
    private val DEVICE_ID = "watch_001"

    private fun sendAlert(
        userId: String,
        logTag: String,
        eventCode: String,
        severity: String = "LOW",
        message: String? = null,
        requiresUserConfirmation: Boolean? = null,
        userResponded: Boolean? = null,
        confirmationWindowSec: Int? = null,
        wearingStatus: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        location: JSONObject? = null,
        heartRateBpm: Int? = null
    ) {
        Log.d("NETWORK", "Calling backend alert API: $logTag")
        Log.e("NETWORK_DEBUG", "HITTING URL: $BASE_URL/api/alert | eventCode=$eventCode")

        val now = System.currentTimeMillis()
        val eventId = "evt_$now" // good enough for prototype

        val metadata = JSONObject().apply {
            // Optional fields only if provided
            if (!message.isNullOrBlank()) put("alertMessage", message)

            requiresUserConfirmation?.let { put("requiresUserConfirmation", it) }
            userResponded?.let { put("userResponded", it) }
            confirmationWindowSec?.let { put("confirmationWindowSec", it) }

            if (!contactName.isNullOrBlank()) put("contactName", contactName)
            if (!contactPhone.isNullOrBlank()) put("contactPhone", contactPhone)

            // sensorState subobject (only include if we have something)
            if (!wearingStatus.isNullOrBlank()) {
                put("sensorState", JSONObject().apply {
                    put("wearingStatus", wearingStatus)
                })
            }
            location?.let { put("location", it) }
            heartRateBpm?.let { bpm ->
                put("vitals", JSONObject().apply {
                    put("heartRateBpm", bpm)
                })
            }

        }

        val json = JSONObject().apply {
            // NEW structured fields
            put("eventId", eventId)
            put("deviceId", DEVICE_ID)
            put("userId", userId)
            put("eventCode", eventCode)
            put("severity", severity)
            put("timestamp", now)
            put("metadata", metadata)

            // LEGACY
            put("alertType", eventCode)
        }

        Log.d("NETWORK_DEBUG", "Alert payload: ${json}")

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

    fun sendWatchRemovedAlert(
        userId: String,
        message: String? = null,
        wearingStatus: String? = null,
        location: JSONObject? = null,
    ) = sendAlert(
        userId = userId,
        logTag = "watch-removed",
        eventCode = "WATCH_REMOVED",
        severity = "LOW",
        message = message,
        wearingStatus = wearingStatus,
        location = location
    )


    fun sendFallDetectedAlert(
        userId: String,
        message: String? = null,
        severity: String = "MEDIUM",
        requiresUserConfirmation: Boolean = true,
        userResponded: Boolean = false,
        confirmationWindowSec: Int = 5,
        wearingStatus: String? = null,
        location: JSONObject? = null
    ) = sendAlert(
        userId = userId,
        logTag = "fall-detected",
        eventCode = "FALL_DETECTED",
        severity = severity,
        message = message,
        requiresUserConfirmation = requiresUserConfirmation,
        userResponded = userResponded,
        confirmationWindowSec = confirmationWindowSec,
        wearingStatus = wearingStatus,
        location = location
    )

    fun sendFallNoResponseAlert(
        userId: String,
        elapsedMs: Long,
        wearingStatus: String? = null,
        location: JSONObject? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        heartRateBpm: Int? = null
    ) = sendAlert(
        userId = userId,
        logTag = "fall-no-response",
        eventCode = "FALL_NO_RESPONSE",
        severity = "HIGH",
        message = "High severity fall: no response after ${elapsedMs/1000}s",
        requiresUserConfirmation = true,
        userResponded = false,
        confirmationWindowSec = (elapsedMs / 1000).toInt(),
        wearingStatus = wearingStatus,
        location = location,
        contactName = contactName,
        contactPhone = contactPhone,
        heartRateBpm = heartRateBpm
    )

    fun sendFallConfirmedHelpAlert(
        userId: String,
        confirmationWindowSec: Int,
        wearingStatus: String? = null,
        location: JSONObject? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        heartRateBpm: Int? = null,
    ) = sendAlert(
        userId = userId,
        logTag = "fall-confirmed",
        eventCode = "FALL_CONFIRMED_HELP",
        severity = "HIGH",
        message = "Fall confirmed by user: needs immediate attention",
        requiresUserConfirmation = true,
        userResponded = true,
        confirmationWindowSec = confirmationWindowSec,
        wearingStatus = wearingStatus,
        location = location,
        contactName = contactName,
        contactPhone = contactPhone,
        heartRateBpm = heartRateBpm
    )




    fun panicButtonPressed(
        userId: String,
        message: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        location: JSONObject? = null,
    ) =
        sendAlert(
            userId = userId,
            logTag = "panic-button",
            eventCode = "PANIC",
            severity = "HIGH",
            message = message ?: "Panic button pressed",
            requiresUserConfirmation = false,
            contactName = contactName,
            contactPhone = contactPhone,
            location = location
        )

    fun sendDangerousHeartRateAlert(userId: String, message: String? = null) =
        sendAlert(
            userId = userId,
            logTag = "dangerous-heart-rate",
            eventCode = "DANGEROUS_HR",
            severity = "MEDIUM",
            message = message ?: "Dangerous heart rate detected",
        )

    fun sendEmergencyCallAlert(
        userId: String,
        contactName: String,
        contactPhone: String,
        message: String? = null
    ) =
        sendAlert(
            userId = userId,
            logTag = "emergency-call",
            eventCode = "EMERGENCY_CALL",
            severity = "CRITICAL",
            message = message ?: "Escalating to emergency call",
            contactName = contactName,
            contactPhone = contactPhone
        )
}

