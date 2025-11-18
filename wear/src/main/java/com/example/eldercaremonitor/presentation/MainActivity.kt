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

import androidx.health.services.client.HealthServices
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager

class MainActivity : ComponentActivity() {

    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager

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
                        wearingText = "Status: ⚠️ Watch removed!"
                        vibrateWarning()
                        showWatchRemovedNotification()
                        heartRateManager.stop()
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
