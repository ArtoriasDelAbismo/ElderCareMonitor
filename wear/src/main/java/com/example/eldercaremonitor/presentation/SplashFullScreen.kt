package com.example.eldercaremonitor.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.eldercaremonitor.R


@Composable
fun FullScreenSplash(onFinished: () -> Unit) {

    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500) // How long the splash stays
        visible = false
        delay(350)  // Duration of fade-out
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF17233C)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.homebrella_full_splash),
                contentDescription = "Homebrella Splash",
                modifier = Modifier.size(320.dp)
            )
        }
    }
}

