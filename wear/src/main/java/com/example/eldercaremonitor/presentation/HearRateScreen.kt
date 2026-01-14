package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eldercaremonitor.R

@Composable
fun HeartRateScreen(
    hr: Int?,
    wearingStatus: String,
    onDebugFall: () -> Unit,
    onPanic: () -> Unit
) {

    val hasBpm = hr != null && hr > 0

    val bpmFontSize = if (hasBpm) 48.sp else 24.sp


    Column(
        modifier = Modifier
            .background(Color(0xFF17233C))
            .fillMaxSize()
            .padding(12.dp),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ICON
        Image(
            painter = painterResource(id = R.drawable.homebrella_icon_cropped),
            contentDescription = "Homebrella Icon",
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // BPM + HEART ICON + "bpm"
        if (hasBpm) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Text(
                    text = hr.toString(),
                    textAlign = TextAlign.Center,
                    fontSize = bpmFontSize,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(4.dp))

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 18.sp
                    )
                    Text(
                        text = "bpm",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }


            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (wearingStatus == "Status: Removed ❌") {
            Text(
                text = wearingStatus,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }


        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "-->",
            textAlign = TextAlign.End,
            color = Color.White
        )


        // BUTTON TO SIMULATE FALL

        //Button(onClick = onDebugFall, modifier = Modifier.width(100.dp)) {
        //    Text(text = "Simulate fall")
        //}
    }
}

