package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.overlay.NudgeQueueManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nudgeId = intent.getLongExtra(EXTRA_NUDGE_ID, -1L)
        Log.d(TAG, "Alarm received for nudgeId: $nudgeId")

        if (nudgeId <= 0) return

        // Wake screen
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "nudge:alarm_receiver_wake"
            )
            wakeLock.acquire(15 * 1000L) // 15 seconds
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock in AlarmReceiver", e)
        }

        // Add to queue and display
        NudgeQueueManager.enqueue(nudgeId, context)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_FIRE_NUDGE = "com.example.nudge.ACTION_FIRE_NUDGE"
        const val EXTRA_NUDGE_ID = "extra_nudge_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }
}
