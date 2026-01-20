package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.example.eldercaremonitor.R


@Composable
fun DangerousHrSuggestionScreen(
    onImOk: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF17233C))
            .padding(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(painter = painterResource(
                id = R.drawable.homebrella_icon_cropped),
                contentDescription = "Homebrella Icon",
                modifier = Modifier.size(48.dp)
            )
        }
        item {
            Spacer(modifier = Modifier.size(10.dp))
        }
        item {
            Text(
                text = "High heart rate",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 18.sp
            )
        }
        item {
            Spacer(modifier = Modifier.size(6.dp))
        }
        item {
            Text(
                text = "Slow down and rest for a moment.",
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
        item {
            Spacer(modifier = Modifier.size(6.dp))
        }
        item {
            Text(
                text = "Breathe slowly and resume when you feel better.",
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
        item {
            Spacer(modifier = Modifier.size(16.dp))
        }
        item {
            Button(onClick = onImOk, modifier = Modifier.width(50.dp)) {
                Text(text = "Ok")
            }
        }



    }

}
