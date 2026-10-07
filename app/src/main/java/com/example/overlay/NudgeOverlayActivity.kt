package com.example.overlay

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.alarm.AlarmScheduler
import com.example.data.NudgeDatabase
import com.example.data.NudgeEntity
import com.example.data.NudgeRepository
import com.example.ui.NudgeOverlayContent
import com.example.util.NudgeSoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class NudgeOverlayActivity : ComponentActivity() {

    private lateinit var repository: NudgeRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private val currentNudgeState = MutableStateFlow<NudgeEntity?>(null)
    private var nudgeId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        configureLockScreenDisplay()

        val dao = NudgeDatabase.getDatabase(applicationContext).nudgeDao()
        repository = NudgeRepository(dao)
        alarmScheduler = AlarmScheduler(applicationContext)

        nudgeId = intent.getLongExtra(EXTRA_NUDGE_ID, -1L)
        if (nudgeId <= 0) {
            finish()
            return
        }

        NudgeSoundPlayer.startAlarmSound(applicationContext, lifecycleScope)

        lifecycleScope.launch {
            val nudge = repository.getNudgeById(nudgeId)
            if (nudge != null) {
                currentNudgeState.value = nudge
            } else {
                finish()
            }
        }

        setContent {
            val nudge by currentNudgeState.collectAsState()
            NudgeOverlayContent(
                nudge = nudge,
                onDone = {
                    handleDone()
                },
                onSnoozeMinutes = { minutes ->
                    handleSnooze(minutes)
                }
            )
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun handleDone() {
        NudgeSoundPlayer.stopAlarmSound(applicationContext)
        val idToFinish = nudgeId

        lifecycleScope.launch {
            repository.markCompleted(idToFinish)
            NudgeQueueManager.onNudgeDismissed(idToFinish, applicationContext)
            finish()
        }
    }

    private fun handleSnooze(minutes: Int) {
        NudgeSoundPlayer.stopAlarmSound(applicationContext)
        val idToSnooze = nudgeId

        lifecycleScope.launch {
            val newScheduledTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.snooze(idToSnooze, newScheduledTime)

            val updatedNudge = repository.getNudgeById(idToSnooze)
            if (updatedNudge != null) {
                alarmScheduler.scheduleNudge(updatedNudge)
            }

            NudgeQueueManager.onNudgeDismissed(idToSnooze, applicationContext)
            finish()
        }
    }

    override fun onDestroy() {
        NudgeSoundPlayer.stopAlarmSound(applicationContext)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NUDGE_ID = "extra_nudge_id"
    }
}
