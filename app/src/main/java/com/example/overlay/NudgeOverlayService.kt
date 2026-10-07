package com.example.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.NudgeApplication
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.NudgeDatabase
import com.example.data.NudgeEntity
import com.example.data.NudgeRepository
import com.example.ui.NudgeOverlayContent
import com.example.util.NudgeSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class NudgeOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private lateinit var repository: NudgeRepository
    private lateinit var alarmScheduler: AlarmScheduler

    private val currentNudgeState = MutableStateFlow<NudgeEntity?>(null)
    private var currentNudgeId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        val dao = NudgeDatabase.getDatabase(applicationContext).nudgeDao()
        repository = NudgeRepository(dao)
        alarmScheduler = AlarmScheduler(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_HIDE_NUDGE) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        val nudgeId = intent?.getLongExtra(EXTRA_NUDGE_ID, -1L) ?: -1L
        if (nudgeId > 0) {
            currentNudgeId = nudgeId
            startForeground(NudgeApplication.NOTIFICATION_ID_SERVICE, createNotification())
            loadNudgeAndShowOverlay(nudgeId)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun loadNudgeAndShowOverlay(nudgeId: Long) {
        serviceScope.launch {
            val nudge = repository.getNudgeById(nudgeId)
            if (nudge != null) {
                currentNudgeState.value = nudge
                showOrUpdateOverlay()
                NudgeSoundPlayer.startAlarmSound(applicationContext, serviceScope)
            } else {
                Log.e(TAG, "Nudge $nudgeId not found in DB")
                NudgeQueueManager.onNudgeDismissed(nudgeId, applicationContext)
            }
        }
    }

    private fun showOrUpdateOverlay() {
        if (overlayView == null) {
            val newLifecycleOwner = OverlayLifecycleOwner()
            lifecycleOwner = newLifecycleOwner

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(newLifecycleOwner)
                setViewTreeViewModelStoreOwner(newLifecycleOwner)
                setViewTreeSavedStateRegistryOwner(newLifecycleOwner)

                setContent {
                    val nudge by currentNudgeState.collectAsState()
                    NudgeOverlayContent(
                        nudge = nudge,
                        onDone = { handleDone() },
                        onSnoozeMinutes = { minutes -> handleSnooze(minutes) }
                    )
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager?.addView(composeView, params)
                overlayView = composeView
                newLifecycleOwner.handleResume()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add WindowManager overlay view", e)
                // Fallback to activity if overlay permission is revoked
                NudgeQueueManager.launchOverlayActivity(currentNudgeId, applicationContext)
            }
        }
    }

    private fun handleDone() {
        NudgeSoundPlayer.stopAlarmSound(applicationContext)
        val idToFinish = currentNudgeId

        serviceScope.launch {
            repository.markCompleted(idToFinish)
            removeOverlay()
            NudgeQueueManager.onNudgeDismissed(idToFinish, applicationContext)
        }
    }

    private fun handleSnooze(minutes: Int) {
        NudgeSoundPlayer.stopAlarmSound(applicationContext)
        val idToSnooze = currentNudgeId

        serviceScope.launch {
            val newScheduledTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.snooze(idToSnooze, newScheduledTime)

            val updatedNudge = repository.getNudgeById(idToSnooze)
            if (updatedNudge != null) {
                alarmScheduler.scheduleNudge(updatedNudge)
            }

            removeOverlay()
            NudgeQueueManager.onNudgeDismissed(idToSnooze, applicationContext)
        }
    }

    private fun removeOverlay() {
        NudgeSoundPlayer.stopAlarmSound(applicationContext)
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
            overlayView = null
        }
        lifecycleOwner?.handleDestroy()
        lifecycleOwner = null
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NudgeApplication.CHANNEL_OVERLAY_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nudge Active")
            .setContentText("Screen hijacked for scheduled task")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        removeOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "NudgeOverlayService"
        const val ACTION_SHOW_NUDGE = "com.example.nudge.ACTION_SHOW_NUDGE"
        const val ACTION_HIDE_NUDGE = "com.example.nudge.ACTION_HIDE_NUDGE"
        const val EXTRA_NUDGE_ID = "extra_nudge_id"
    }
}
