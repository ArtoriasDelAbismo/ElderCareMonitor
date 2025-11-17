package com.example.eldercaremonitor.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.wear.compose.material.Text
import com.example.eldercaremonitor.presentation.theme.ElderCareMonitorTheme

class MainActivity : ComponentActivity() {

    private lateinit var measureClient: MeasureClient
    private var heartRateText by mutableStateOf("Waiting...")

    // Correct callback for beta03
    private val hrCallback = object : MeasureCallback {
        override fun onDataReceived(data: DataPointContainer) {
            val hr = data.getData(DataType.HEART_RATE_BPM).firstOrNull()
            if (hr != null) {
                val bpm = hr.value.toInt()
                heartRateText = "❤️ $bpm bpm"
                Log.d("HEART", "HR = $bpm")
            }
        }

        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            Log.d("AVAIL", "Availability changed: $dataType, $availability")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        measureClient = HealthServices.getClient(this).measureClient

        setContent {
            ElderCareMonitorTheme {

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) startHeartRate()
                        else heartRateText = "Permission denied"
                    }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BODY_SENSORS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startHeartRate()
                    } else {
                        permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                    }
                }

                HeartRateScreen(heartRateText)
            }
        }
    }

    private fun startHeartRate() {
        measureClient.registerMeasureCallback(
            DataType.HEART_RATE_BPM,
            hrCallback
        )

        heartRateText = "Measuring..."
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister correctly for beta03
        measureClient.unregisterMeasureCallbackAsync(
            DataType.HEART_RATE_BPM,
            hrCallback
        )
    }


}

@Composable
fun HeartRateScreen(hr: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = hr, textAlign = TextAlign.Center)
    }
}
