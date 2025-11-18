package com.example.eldercaremonitor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log

import androidx.health.services.client.HealthServices
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException


class MainActivity : ComponentActivity() {

    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager
    private val userId = "elder_001"

    // Vibration warning
    private fun vibrateWarning() {
        @Suppress("DEPRECATION")
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(600) // simple vibration
    }

    // Notification when watch removed
    private fun showWatchRemovedNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wearing_alerts",
                "Wearing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // For Android 13+, ensure POST_NOTIFICATIONS permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Notifications permission not granted", Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }

        val notification = NotificationCompat.Builder(this, "wearing_alerts")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Watch Removed")
            .setContentText("The device is no longer being worn.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1001, notification)
    }


// Send alert to backend

    private fun sendWatchRemovedAlert() {
        Log.d("NETWORK", "Calling backend alert API")

        val client = OkHttpClient()
        val json = JSONObject()
        json.put("userId", userId)
        json.put("timestamp", System.currentTimeMillis())


        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.8:3001/api/alert/watch-removed")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val measureClient = HealthServices.getClient(this).measureClient

        setContent {
            var hrText by remember { mutableStateOf("Waiting...") }
            var wearingText by remember { mutableStateOf("Detecting...") }

            // Heart Rate Manager

            heartRateManager = HeartRateManager(
                measureClient = measureClient,
                onHeartRateChanged = { bpm ->
                    hrText = "❤️ $bpm bpm"
                },
                onDangerousHeartRate = { bpm ->
                    hrText = "⚠️ DANGEROUS HR: $bpm bpm"
                }
            )

            // Wearing State Manager

            wearingManager = remember {
                WearingStateManager(
                    context = this@MainActivity,
                    onWorn = {
                        wearingText = "Status: Wearing ✅"
                        heartRateManager.start()
                    },
                    onRemoved = {
                        Log.d("WEARING", "Watch was removed!")

                        wearingText = "Status: ⚠️ Watch removed!"
                        vibrateWarning()
                        showWatchRemovedNotification()
                        heartRateManager.stop()
                        sendWatchRemovedAlert()
                    }
                )
            }

            // Request BODY_SENSORS permission

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        heartRateManager.start()
                        wearingManager.start()
                    } else {
                        hrText = "Permission denied"
                        wearingText = "Permission denied"
                    }
                }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BODY_SENSORS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    heartRateManager.start()
                    wearingManager.start()
                } else {
                    permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                }
            }


            // Request POST_NOTIFICATIONS permission (Android 13+)

            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (!granted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Notifications permission not granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }


            // Compose UI

            ElderCareMonitorTheme {
                HeartRateScreen(
                    hr = hrText,
                    wearingStatus = wearingText
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateManager.stop()
        wearingManager.stop()
    }
}


