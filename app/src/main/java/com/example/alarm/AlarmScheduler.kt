package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.NudgeEntity
import com.example.receiver.AlarmReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNudge(nudge: NudgeEntity) {
        val now = System.currentTimeMillis()
        if (nudge.scheduledTimeMillis <= now) {
            Log.w(TAG, "Scheduled time is in the past: ${nudge.scheduledTimeMillis} <= $now")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_NUDGE
            putExtra(AlarmReceiver.EXTRA_NUDGE_ID, nudge.id)
            putExtra(AlarmReceiver.EXTRA_TASK_NAME, nudge.taskName)
            putExtra(AlarmReceiver.EXTRA_SCHEDULED_TIME, nudge.scheduledTimeMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            nudge.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nudge.scheduledTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nudge.scheduledTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nudge.scheduledTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled exact alarm for nudge ${nudge.id} at ${nudge.scheduledTimeMillis}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm", e)
        }
    }

    fun cancelNudge(nudgeId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_NUDGE
            putExtra(AlarmReceiver.EXTRA_NUDGE_ID, nudgeId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            nudgeId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for nudge $nudgeId")
        }
    }

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
