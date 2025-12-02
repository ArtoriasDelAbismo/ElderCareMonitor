package com.example.eldercaremonitor.presentation
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.health.services.client.HealthServices
import com.example.eldercaremonitor.presentation.FallCheckScreen
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.presentation.utils.NotificationHelper
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager
import com.example.eldercaremonitor.presentation.utils.VibrationHelper
import com.example.eldercaremonitor.sensors.FallDetectionManager
import data.network.AlertService


class MainActivity : ComponentActivity() {

    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager
    private lateinit var fallDetectionManager: FallDetectionManager
    private lateinit var showFallDetectedNotification: NotificationHelper
    private lateinit var vibrateWarning: VibrationHelper
    private lateinit var showWatchRemovedNotification: NotificationHelper
    private lateinit var sendWatchRemovedAlert: AlertService
    private lateinit var sendFallDetectedAlert: AlertService

    private val userId = "elder_001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        vibrateWarning = VibrationHelper(this)
        showWatchRemovedNotification = NotificationHelper(this)//-----NOT USING IT RIGHT NOW-----
        sendWatchRemovedAlert = AlertService()
        showFallDetectedNotification = NotificationHelper(this)
        sendFallDetectedAlert = AlertService()

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
                        vibrateWarning.vibrate()
                        heartRateManager.stop()
                        sendWatchRemovedAlert.sendWatchRemovedAlert(userId)
                    }
                )
            }

            // Fall Detection Manager

            fallDetectionManager = FallDetectionManager(
                context = this@MainActivity,
                onFallDetected = {
                    vibrateWarning.vibrate()
                    Log.d("FALL", "Fall detected!")
                    sendFallDetectedAlert.sendFallDetectedAlert("FALL DETECTED: $userId")
                    showFallDetectedNotification.showFallDetectedNotification()
                }
            )

            // Request BODY_SENSORS permission

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        heartRateManager.start()
                        wearingManager.start()
                        fallDetectionManager.start()
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
                    fallDetectionManager.start()
                    hrText = "Waiting HR..."
                    wearingText = "Waiting Wear..."
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
        fallDetectionManager.stop()
    }
}


