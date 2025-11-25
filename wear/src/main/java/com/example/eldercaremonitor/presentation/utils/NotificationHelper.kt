package com.example.eldercaremonitor.presentation.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context){
    fun showWatchRemovedNotification() {
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wearing_alerts",
                "Wearing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // For Android 13+, ensure POST_NOTIFICATIONS permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Notifications permission not granted", Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }

        val notification = NotificationCompat.Builder(context, "wearing_alerts")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Watch Removed")
            .setContentText("The device is no longer being worn.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1001, notification)
    }
}