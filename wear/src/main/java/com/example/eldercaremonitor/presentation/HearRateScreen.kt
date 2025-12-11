package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Text
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button

@Composable
fun HeartRateScreen(hr: String, wearingStatus: String, onDebugFall: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = hr, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = wearingStatus, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onDebugFall, modifier = Modifier.width(100.dp)) {
            Text(text = "Simulate fall")
        }
    }
}
