package com.example.eldercaremonitor.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.net.Uri
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
//import androidx.health.services.client.data.DataType
//import androidx.health.services.client.permission.HealthPermission
//import androidx.health.services.client.permission.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme
import com.example.eldercaremonitor.presentation.utils.NotificationHelper
import com.example.eldercaremonitor.presentation.utils.VibrationHelper
import com.example.eldercaremonitor.presentation.utils.panicLongPress
import com.example.eldercaremonitor.sensors.FallDetectionManager
import com.example.eldercaremonitor.sensors.HeartRateManager
import com.example.eldercaremonitor.sensors.WearingStateManager
import com.example.eldercaremonitor.data.location.WatchLocationHelper
import kotlinx.coroutines.Dispatchers
import data.network.AlertService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import safety.SafetyEngine
import safety.SafetyEvent

class MainActivity : ComponentActivity() {
    private companion object {
        private const val FALL_NO_RESPONSE_TIMEOUT_MS = 15_000L
    }

    @Volatile
    private var hasLocationPermissionFlag: Boolean = false
    private lateinit var heartRateManager: HeartRateManager
    private lateinit var wearingManager: WearingStateManager
    private lateinit var fallDetectionManager: FallDetectionManager
    private lateinit var safetyEngine: SafetyEngine
    private lateinit var alertService: AlertService
    //private lateinit var permissionController: PermissionController
    //private lateinit var requestBodySensorsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    //private lateinit var requestHealthServicesPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
    //private var hasBodySensorsPermission = false
    //private var hasHeartRatePermission = false

    private val userId = "elder_001"

    // ✅ Activity-owned Compose state (bridge)
    private val hrTextState = mutableStateOf<String?>(null)
    private val wearingTextState = mutableStateOf("Status: Detecting")
    private val showFallCheckState = mutableStateOf(false)

    private val showDangerousHrSuggestionState = mutableStateOf(false)

    private val emergencyContacts = listOf(
        EmergencyContact("Jero", "+5493425145911"), // My iphone
        EmergencyContact("Juank", "+543424070425"), // Juank's iphone
        EmergencyContact("Android", "+5493425925234"), // Android phone
        EmergencyContact("Freddy", "0987654321"),

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("BOOT", "MainActivity.onCreate START")
        super.onCreate(savedInstanceState)
        Log.d("BOOT", "MainActivity.onCreate AFTER super")

        installSplashScreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alertService = AlertService()
        //permissionController = HealthServices.getClient(this).permissionController
        safetyEngine = SafetyEngine(
            vibrateWarning = VibrationHelper(this),
            showWatchRemovedNotification = NotificationHelper(this),
            showFallDetectedNotification = NotificationHelper(this),
            showDangerousHeartRateNotification = NotificationHelper(this),
            showPanicButtonPressedNotification = NotificationHelper(this),
            alertService = alertService,
            userId = userId,
            locationHelper = WatchLocationHelper(this),
            hasLocationPermission = { hasLocationPermissionFlag },
            getPrimaryContact = { emergencyContacts.firstOrNull() }
        )

        val measureClient = HealthServices.getClient(this).measureClient

        // ✅ Managers live here (Activity lifecycle)
        heartRateManager = HeartRateManager(
            measureClient = measureClient,
            onHeartRateChanged = { bpm ->
                hrTextState.value = bpm.toString()
                safetyEngine.onEvent(SafetyEvent.HeartRate(bpm))
            },
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

        wearingManager.start()
        fallDetectionManager.start()


        /*
                requestBodySensorsPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasBodySensorsPermission = granted
                if (!granted) {
                    Toast.makeText(
                        this,
                        "Body sensors permission not granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                requestHealthServicesPermission()
            }

        requestHealthServicesPermissionLauncher =
            registerForActivityResult(
                PermissionController.createRequestPermissionActivityContract()
            ) { grantedPermissions ->
                val hrPermission = HealthPermission.getReadPermission(DataType.HEART_RATE_BPM)
                hasHeartRatePermission = grantedPermissions.contains(hrPermission)
                if (!hasHeartRatePermission) {
                    Toast.makeText(
                        this,
                        "Heart rate permission not granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                startSensorsIfReady()
            }

        lifecycleScope.launch {
            hasBodySensorsPermission =
                checkSelfPermission(Manifest.permission.BODY_SENSORS) ==
                    PackageManager.PERMISSION_GRANTED
            val hrPermission = HealthPermission.getReadPermission(DataType.HEART_RATE_BPM)
            val grantedPermissions = permissionController.getGrantedPermissions()
            hasHeartRatePermission = grantedPermissions.contains(hrPermission)

            if (!hasBodySensorsPermission) {
                requestBodySensorsPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                return@launch
            }

            if (!hasHeartRatePermission) {
                requestHealthServicesPermission()
                return@launch
            }

            startSensorsIfReady()
        }

        */




        setContent {
            val hrText = hrTextState.value
            val wearingText = wearingTextState.value
            val showFallCheckScreen = showFallCheckState.value
            val showDangerousHrSuggestion = showDangerousHrSuggestionState.value



            val emergencyContacts = remember {
                listOf(
                    EmergencyContact("Anna", "+5493425925234"),
                    EmergencyContact("Jero", "3425145911"),
                    EmergencyContact("Freddy", "0987654321"),

                )
            }
            val pendingCallContact = remember { mutableStateOf<EmergencyContact?>(null) }

            LaunchedEffect(showFallCheckScreen) {
                if (showFallCheckScreen) {
                    delay(FALL_NO_RESPONSE_TIMEOUT_MS)
                    if (showFallCheckState.value) {
                        showFallCheckState.value = false
                        fallDetectionManager.reset()
                        safetyEngine.onEvent(
                            SafetyEvent.FallNoResponse(FALL_NO_RESPONSE_TIMEOUT_MS)
                        )
                        val contact = emergencyContacts.firstOrNull()
                        if (contact != null) {
                            launchEmergencyCall(contact)
                        } else {
                            Log.w("EMERGENCY", "No emergency contacts available for auto-escalation")
                        }
                    }
                }
            }

            // NOTIFICATIONS PERMISSIONS

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

            //  ---- PERMISSIONS ONLY IF IMPLEMENTING ACTION_CALL ----
                val callPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val contact = pendingCallContact.value
                    if (granted && contact != null) {
                        pendingCallContact.value = null
                        launchEmergencyCall(contact)
                    } else if (!granted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Call permission not granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            // ---- PERMISSIONS FOR GPS LOCATION ----

            var hasLocationPermission by remember {
                mutableStateOf(
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                )
            }

            // LAUNCHER

            val locationPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    hasLocationPermission = fine || coarse

                    hasLocationPermissionFlag = hasLocationPermission


                    if (!hasLocationPermission) {
                        Toast.makeText(
                            this@MainActivity,
                            "Location permission not granted",
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
                // val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                hasLocationPermission = coarse
                hasLocationPermissionFlag = hasLocationPermission
                Log.d("LOCATION", "Init permission: $hasLocationPermissionFlag")



                if (!hasLocationPermission) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    hasLocationPermission = true
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

                        showDangerousHrSuggestion -> {
                            DangerousHrSuggestionScreen(
                                onImOk = {
                                    showDangerousHrSuggestionState.value = false
                                    safetyEngine.onEvent(SafetyEvent.UserIsOk)
                                }
                            )
                        }
                        else -> {
                            PagerScreen(
                                heartRate = hrText?.toIntOrNull(),
                                wearingStatus = wearingText,
                                contacts = emergencyContacts,
                                onCallContact = { contact ->
                                    Log.d("EMERGENCY", "Selected contact: ${contact.name}")

                                    if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                                        == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        launchEmergencyCall(contact)
                                    } else {
                                        pendingCallContact.value = contact
                                        callPermissionLauncher.launch(
                                            Manifest.permission.CALL_PHONE
                                        )
                                    }
                                },
                                onPanic = {
                                    safetyEngine.onEvent(SafetyEvent.PanicButtonPressed)
                                },
                                onDebugFall = {
                                    showFallCheckState.value = true
                                    safetyEngine.onEvent(SafetyEvent.FallDetected)
                                },
                                onDangerousHrSuggestion = {
                                    showDangerousHrSuggestionState.value = true
                                }
                            )
                        }
                    }

                }

            }
        }
        Log.d("BOOT", "MainActivity.onCreate END")
    }

    override fun onDestroy() {
        Log.d("BOOT", "MainActivity.onDestroy")
        safetyEngine.clear()
        super.onDestroy()
        heartRateManager.stop()
        wearingManager.stop()
        fallDetectionManager.stop()
    }


    private fun launchEmergencyCall(contact: EmergencyContact) {
        val phoneNumber = contact.phoneNumber
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        try {
            startActivity(callIntent)
            alertService.sendEmergencyCallAlert(
                userId = userId,
                contactName = contact.name,
                contactPhone = phoneNumber,
                message = "Emergency call started"
            )
        } catch (e: Exception) {
            Log.e("EMERGENCY", "Failed to start emergency call", e)
            Toast.makeText(
                this@MainActivity,
                "Unable to start emergency call",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
    private fun requestHealthServicesPermission() {
        val hrPermission = HealthPermission.getReadPermission(DataType.HEART_RATE_BPM)
        requestHealthServicesPermissionLauncher.launch(setOf(hrPermission))
    }

    private fun startSensorsIfReady() {
        if (!hasBodySensorsPermission || !hasHeartRatePermission) return
        wearingManager.start()
        heartRateManager.start()
        fallDetectionManager.start()
    }
    */


}
