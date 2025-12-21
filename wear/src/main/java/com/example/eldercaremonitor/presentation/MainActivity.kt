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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.services.client.HealthServices
import com.example.eldercaremonitor.presentation.FallCheckScreen
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.presentation.utils.NotificationHelper
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager
import com.example.eldercaremonitor.presentation.utils.VibrationHelper
import com.example.eldercaremonitor.sensors.FallDetectionManager
import data.network.AlertService
import safety.SafetyEngine
import safety.SafetyEvent


class MainActivity : ComponentActivity() {
    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager
    private lateinit var fallDetectionManager: FallDetectionManager
    private lateinit var safetyEngine: SafetyEngine


    private val userId = "elder_001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        safetyEngine = SafetyEngine(
            vibrateWarning = VibrationHelper(this),
            showWatchRemovedNotification = NotificationHelper(this),
            showFallDetectedNotification = NotificationHelper(this),
            alertService = AlertService(),
            userId = userId
        )


        val measureClient = HealthServices.getClient(this).measureClient

        setContent {
            var hrText by remember { mutableStateOf("Waiting HR") }
            var wearingText by remember { mutableStateOf("Status: Detecting") }
            var showFallCheckScreen by remember { mutableStateOf(false) }

            // HEART RATE MANAGER

            heartRateManager = HeartRateManager(
                measureClient = measureClient,
                onHeartRateChanged = { bpm ->
                    hrText = "$bpm"
                },
                onDangerousHeartRate = { bpm ->
                    safetyEngine.onEvent(SafetyEvent.DangerousHeartRate(bpm))
                }
            )

            // WEARING STATE MANAGER

            wearingManager = remember {
                WearingStateManager(
                    context = this@MainActivity,
                    onWorn = {
                        wearingText = "Status: Wearing ✅"
                        heartRateManager.start()
                    },
                    onRemoved = {
                        safetyEngine.onEvent(SafetyEvent.WatchRemoved)
                        heartRateManager.stop()
                    }
                )
            }

            // FALL DETECTION MANAGER

            fallDetectionManager = FallDetectionManager(
                context = this@MainActivity,
                onFallDetected = {
                    safetyEngine.onEvent(SafetyEvent.FallDetected)
                    showFallCheckScreen = true
                }
            )

            // REQUEST BODY_SENSORS PERMISSION

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
                    //hrText = "Waiting HR..."
                    //wearingText = "Waiting Wear..."
                } else {
                    permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                }
            }

            // REQUEST POST NOTIFICATIONS PERMISSION

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

            // COMPOSE UI

            ElderCareMonitorTheme {

                var showFullSplash by remember { mutableStateOf(true) }

                when {
                    //  Show full screen splash first
                    showFullSplash -> {
                        FullScreenSplash {
                            showFullSplash = false
                        }
                    }

                    //  Show fall check UI if needed
                    showFallCheckScreen -> {
                        FallCheckScreen(
                            onImOk = {
                                showFallCheckScreen = false
                                fallDetectionManager.reset()

                                safetyEngine.onEvent(SafetyEvent.UserIsOk)
                            },
                            onNeedHelp = {
                                showFallCheckScreen = false
                                fallDetectionManager.reset()

                                safetyEngine.onEvent(SafetyEvent.UserNeedsHelp)
                            }
                        )
                    }

                    //  Default → main heart rate screen
                    else -> {
                        HeartRateScreen(
                            hr = hrText,
                            wearingStatus = wearingText,
                            onDebugFall = {
                                showFallCheckScreen = true
                            },
                            onPanic = {
                                safetyEngine.onEvent(SafetyEvent.PanicButtonPressed)
                            }
                        )
                    }
                }
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

