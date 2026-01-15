package com.example.eldercaremonitor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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

    // ✅ Activity-owned Compose state (bridge)
    private val hrTextState = mutableStateOf<String?>(null)
    private val wearingTextState = mutableStateOf("Status: Detecting")
    private val showFallCheckState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        // ✅ Managers live here (Activity lifecycle)
        heartRateManager = HeartRateManager(
            measureClient = measureClient,
            onHeartRateChanged = { bpm ->
                hrTextState.value = bpm.toString()
                safetyEngine.onEvent(SafetyEvent.HeartRate(bpm))
            },
            onDangerousHeartRate = { bpm ->
                safetyEngine.onEvent(SafetyEvent.DangerousHeartRate(bpm))
            }
        )

        wearingManager = WearingStateManager(
            context = this,
            onWorn = {
                wearingTextState.value = "Status: Wearing ✅"
                safetyEngine.onEvent(SafetyEvent.WatchWornAgain)
                heartRateManager.start()
            },
            onRemoved = {
                wearingTextState.value = "Status: Removed ❌"
                safetyEngine.onEvent(SafetyEvent.WatchRemoved)
                heartRateManager.stop()
            }
        )

        fallDetectionManager = FallDetectionManager(
            context = this,
            onFallDetected = {
                showFallCheckState.value = true
                safetyEngine.onEvent(SafetyEvent.FallDetected)
            }
        )

        // Start sensors once
        wearingManager.start()
        heartRateManager.start()
        fallDetectionManager.start()

        setContent {
            val hrText = hrTextState.value
            val wearingText = wearingTextState.value
            val showFallCheckScreen = showFallCheckState.value

            val emergencyContacts = remember {
                listOf(
                    EmergencyContact("Ana", "1234567890"),
                    EmergencyContact("Freddy", "0987654321"),
                    EmergencyContact("Tom", "0987654322"),
                )
            }

            // permissions
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
                                    showFallCheckState.value = false
                                    fallDetectionManager.reset()
                                    safetyEngine.onEvent(SafetyEvent.UserIsOk)
                                },
                                onNeedHelp = {
                                    showFallCheckState.value = false
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
