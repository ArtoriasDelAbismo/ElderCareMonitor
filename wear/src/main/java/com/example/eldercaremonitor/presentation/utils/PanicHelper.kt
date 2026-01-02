package com.example.eldercaremonitor.presentation.utils

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput


fun Modifier.panicLongPress(
    durationMs: Long = 2000,
    onPanic: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown()
            val startTime = System.currentTimeMillis()

            // Wait until finger is released or time passes
            var triggered = false
            while (true) {
                val event = awaitPointerEvent()
                val now = System.currentTimeMillis()

                if (now - startTime >= durationMs) {
                    onPanic()
                    triggered = true
                    break
                }

                if (event.changes.any { it.changedToUp() }) {
                    break
                }
            }

            if (triggered) {
                // Consume remaining events to avoid duplicate triggers
                down.consume()
            }
        }
    }
}
