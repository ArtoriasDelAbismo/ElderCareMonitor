package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

@Composable
fun FallCheckScreen(
    onImOk: () -> Unit,
    onNeedHelp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Fall detected", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Are you okay?", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onImOk) {
            Text(text = "I'm OK")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNeedHelp) {
            Text(text = "Need help")
        }
    }
}
