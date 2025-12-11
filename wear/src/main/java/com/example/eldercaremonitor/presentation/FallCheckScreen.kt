package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Fall detected", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Are you okay?", textAlign = TextAlign.Center)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onImOk, modifier = Modifier.width(50.dp)) {
                Text(text = "I'm ok")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onNeedHelp, modifier = Modifier.width(90.dp)) {
                Text(text = "Need help")
            }
        }
    }

}
