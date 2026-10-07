package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NudgeApplication
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.NudgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d(TAG, "Device booted. Handling missed tasks and rescheduling alarms...")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        scope.launch {
            try {
                val db = NudgeDatabase.getDatabase(appContext)
                val dao = db.nudgeDao()
                val scheduler = AlarmScheduler(appContext)
                val now = System.currentTimeMillis()

                // 1. Check for missed tasks (scheduled time has already passed)
                val missedTasks = dao.getMissedNudges(now)
                if (missedTasks.isNotEmpty()) {
                    Log.d(TAG, "Found ${missedTasks.size} missed tasks")
                    val missedIds = missedTasks.map { it.id }
                    dao.markAsMissed(missedIds)
                    showMissedNotification(appContext, missedTasks.size)
                }

                // 2. Reschedule all upcoming valid alarms
                val upcomingTasks = dao.getUpcomingNudgesList()
                var scheduledCount = 0
                for (task in upcomingTasks) {
                    if (task.scheduledTimeMillis > now) {
                        scheduler.scheduleNudge(task)
                        scheduledCount++
                    }
                }
                Log.d(TAG, "Rescheduled $scheduledCount upcoming nudge alarms successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error in BootReceiver processing", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showMissedNotification(context: Context, count: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "history")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (count == 1) "You missed 1 Nudge" else "You missed $count Nudges"

        val notification = NotificationCompat.Builder(context, NudgeApplication.CHANNEL_MISSED_NUDGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Missed Nudge")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NudgeApplication.NOTIFICATION_ID_MISSED, notification)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
