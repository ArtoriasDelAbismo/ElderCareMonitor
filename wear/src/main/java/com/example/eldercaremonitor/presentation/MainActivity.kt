package com.example.eldercaremonitor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class MainActivity : ComponentActivity() {

    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager


    private fun vibrateWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    600,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(600)
        }
    }

    private fun showWatchRemovedNotification() {
        val manager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wearing_alerts",
                "Wearing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "wearing_alerts")
            .setSmallIcon(android.R.drawable.stat_notify_error)

            .setContentTitle("Watch Removed")
            .setContentText("The device is no longer being worn.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1001, notification)
    }
    // -------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val measureClient = HealthServices.getClient(this).measureClient

        setContent {
            var hrText by remember { mutableStateOf("Waiting...") }
            var wearingText by remember { mutableStateOf("Detecting") }

            // Heart Rate manager
            heartRateManager = HeartRateManager(
                measureClient = measureClient,
                onHeartRateChanged = { bpm ->
                    hrText = "❤️ $bpm bpm"
                },
                onDangerousHeartRate = { bpm ->
                    hrText = "⚠️ DANGEROUS HR: $bpm bpm"
                }
            )

            // Wearing manager inside Compose
            wearingManager = remember {

            WearingStateManager(
                    context = this@MainActivity,
                    onWearingStateChanged = { isWearing ->

                        wearingText = if (isWearing) {
                            "Watch is being worn"
                        } else {
                            "⚠️ Watch removed!"
                        }

                        if (!isWearing) {
                            vibrateWarning()
                            showWatchRemovedNotification()
                            heartRateManager.stop()
                        } else {
                            heartRateManager.start()
                        }
                    }
                )
            }

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
