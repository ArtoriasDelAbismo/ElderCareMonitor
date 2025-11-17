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
import com.example.eldercaremonitor.sensors.HeartRateMonitor

class MainActivity : ComponentActivity() {

    private lateinit var heartRateMonitor: HeartRateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val measureClient = HealthServices.getClient(this).measureClient

        setContent {
            var hrText by remember { mutableStateOf("Waiting...") }

            // Create monitor
            heartRateMonitor = HeartRateMonitor(
                measureClient = measureClient,
                onHeartRateChanged = { bpm ->
                    hrText = "❤️ $bpm bpm"
                },
                onDangerousHeartRate = {bpm ->
                    hrText = "⚠️ DANGEROUS HR: $bpm bpm"
                }
            )

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) heartRateMonitor.start()
                    else hrText = "Permission denied"
                }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BODY_SENSORS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    heartRateMonitor.start()
                } else {
                    permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                }
            }

            ElderCareMonitorTheme {
                HeartRateScreen(hr = hrText)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateMonitor.stop()
    }
}
