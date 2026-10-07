package com.example.overlay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

object NudgeQueueManager {

    private const val TAG = "NudgeQueueManager"
    private val queue = ArrayDeque<Long>()
    private val _currentNudgeId = MutableStateFlow<Long?>(null)
    val currentNudgeId: StateFlow<Long?> = _currentNudgeId.asStateFlow()

    @Synchronized
    fun enqueue(nudgeId: Long, context: Context) {
        wakeDevice(context)

        if (_currentNudgeId.value == nudgeId || queue.contains(nudgeId)) {
            Log.d(TAG, "Nudge $nudgeId is already active or in queue")
            return
        }

        if (_currentNudgeId.value == null) {
            _currentNudgeId.value = nudgeId
            displayNudge(nudgeId, context)
        } else {
            queue.addLast(nudgeId)
            Log.d(TAG, "Queued nudge $nudgeId. Total in queue: ${queue.size}")
        }
    }

    @Synchronized
    fun onNudgeDismissed(nudgeId: Long, context: Context) {
        if (_currentNudgeId.value == nudgeId) {
            if (queue.isNotEmpty()) {
                val nextId = queue.removeFirst()
                _currentNudgeId.value = nextId
                displayNudge(nextId, context)
            } else {
                _currentNudgeId.value = null
                dismissAllOverlays(context)
            }
        }
    }

    private fun displayNudge(nudgeId: Long, context: Context) {
        wakeDevice(context)

        val hasOverlayPermission = Settings.canDrawOverlays(context)
        Log.d(TAG, "Displaying nudge $nudgeId, canDrawOverlays=$hasOverlayPermission")

        if (hasOverlayPermission) {
            val serviceIntent = Intent(context, NudgeOverlayService::class.java).apply {
                action = NudgeOverlayService.ACTION_SHOW_NUDGE
                putExtra(NudgeOverlayService.EXTRA_NUDGE_ID, nudgeId)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting NudgeOverlayService", e)
                // Fallback to overlay activity
                launchOverlayActivity(nudgeId, context)
            }
        } else {
            launchOverlayActivity(nudgeId, context)
        }
    }

    fun launchOverlayActivity(nudgeId: Long, context: Context) {
        val activityIntent = Intent(context, NudgeOverlayActivity::class.java).apply {
            putExtra(NudgeOverlayActivity.EXTRA_NUDGE_ID, nudgeId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        try {
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NudgeOverlayActivity", e)
        }
    }

    private fun dismissAllOverlays(context: Context) {
        val serviceIntent = Intent(context, NudgeOverlayService::class.java).apply {
            action = NudgeOverlayService.ACTION_HIDE_NUDGE
        }
        try {
            context.startService(serviceIntent)
        } catch (ignored: Exception) {}
    }

    private fun wakeDevice(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "nudge:alarm_wake_lock"
            )
            wakeLock.acquire(10 * 1000L) // 10 seconds wake lock
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
}
