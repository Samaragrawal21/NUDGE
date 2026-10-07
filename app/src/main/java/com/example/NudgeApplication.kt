package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NudgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Silent status bar notification channel for missed nudges
            val missedChannel = NotificationChannel(
                CHANNEL_MISSED_NUDGES,
                "Missed Nudges",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent alerts for nudges scheduled while device was off"
                setSound(null, null)
                enableVibration(false)
            }

            // Foreground service channel for the overlay service
            val serviceChannel = NotificationChannel(
                CHANNEL_OVERLAY_SERVICE,
                "Nudge Active Overlay",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active screen hijacking reminder overlay"
            }

            notificationManager.createNotificationChannel(missedChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_MISSED_NUDGES = "nudge_missed_channel"
        const val CHANNEL_OVERLAY_SERVICE = "nudge_service_channel"
        const val NOTIFICATION_ID_MISSED = 1001
        const val NOTIFICATION_ID_SERVICE = 1002
    }
}
