package com.example.eldercaremonitor.presentation.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper(private val context: Context) {

    fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: VibratorManager
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    600,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            // API < 31: Vibrator
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        600,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                // Very old API
                @Suppress("DEPRECATION")
                vibrator.vibrate(600)
            }
        }
    }
}
