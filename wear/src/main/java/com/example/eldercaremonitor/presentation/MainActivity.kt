package com.example.eldercaremonitor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.services.client.HealthServices
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.presentation.utils.NotificationHelper
import com.example.eldercaremonitor.presentation.utils.VibrationHelper
import com.example.eldercaremonitor.presentation.utils.panicLongPress
import com.example.eldercaremonitor.sensors.FallDetectionManager
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager
import data.network.AlertService
import safety.SafetyEngine
import safety.SafetyEvent

class MainActivity : ComponentActivity() {

    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager
    private lateinit var fallDetectionManager: FallDetectionManager
    private lateinit var safetyEngine: SafetyEngine

    private val userId = "elder_001"

    // ---- ON CREATE ----

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        safetyEngine = SafetyEngine(
            vibrateWarning = VibrationHelper(this),
            showWatchRemovedNotification = NotificationHelper(this),
            showFallDetectedNotification = NotificationHelper(this),
            showDangerousHeartRateNotification = NotificationHelper(this),
            showPanicButtonPressedNotification = NotificationHelper(this),
            alertService = AlertService(),
            userId = userId
        )

        val measureClient = HealthServices.getClient(this).measureClient

        setContent {
            var hrText by remember { mutableStateOf<String?>(null) }
            var wearingText by remember { mutableStateOf("Status: Detecting") }
            var showFallCheckScreen by remember { mutableStateOf(false) }
            val emergencyContacts = remember {
                listOf(
                    EmergencyContact("Ana", "1234567890"),
                    EmergencyContact("Freddy", "0987654321"),
                    EmergencyContact("Tom", "0987654322"),
                )
            }

            // HEART RATE MANAGER
            heartRateManager = HeartRateManager(
                measureClient = measureClient,
                onHeartRateChanged = { bpm ->
                    hrText = bpm.toString()
                    safetyEngine.onEvent(SafetyEvent.HeartRate(bpm))
                }
            )


            // WEARING STATE MANAGER
            wearingManager = remember {
                WearingStateManager(
                    context = this@MainActivity,
                    onWorn = {
                        wearingText = "Status: Wearing ✅"
                        safetyEngine.onEvent(SafetyEvent.WatchWornAgain)
                        heartRateManager.start()
                    },
                    onRemoved = {
                        wearingText = "Status: Removed ❌"
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

            // 🔑 START SENSORS IMMEDIATELY
            LaunchedEffect(Unit) {
                wearingManager.start()
                heartRateManager.start()
                fallDetectionManager.start()
            }

            // POST NOTIFICATIONS PERMISSION
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
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            }

            // UI

            ElderCareMonitorTheme {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .panicLongPress {
                            safetyEngine.onEvent(SafetyEvent.PanicButtonPressed)
                        }
                ) {
                    var showFullSplash by remember { mutableStateOf(true) }

                    when {
                        showFullSplash -> {
                            FullScreenSplash { showFullSplash = false }
                        }

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

                        else -> {
                            PagerScreen(
                                heartRate = hrText?.toIntOrNull(),
                                wearingStatus = wearingText,
                                contacts = emergencyContacts,
                                onCallContact = { contact ->
                                    Log.d("EMERGENCY", "Calling ${contact.name}")
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Calling ${contact.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // - launch call intent
                                    // - send backend alert
                                    // - send WhatsApp via Twilio
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
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateManager.stop()
        wearingManager.stop()
        fallDetectionManager.stop()
    }
}
