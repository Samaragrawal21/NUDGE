package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.NudgeDatabase
import com.example.data.NudgeEntity
import com.example.data.NudgeRepository
import com.example.data.NudgeStatus
import com.example.overlay.NudgeQueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class HistoryFilter(val label: String, val durationMillis: Long) {
    LAST_1_DAY("Last 1 Day", 1L * 24 * 60 * 60 * 1000L),
    LAST_7_DAYS("Last 7 Days", 7L * 24 * 60 * 60 * 1000L),
    LAST_30_DAYS("Last 30 Days", 30L * 24 * 60 * 60 * 1000L),
    LAST_6_MONTHS("Last 6 Months", 182L * 24 * 60 * 60 * 1000L)
}

class NudgeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NudgeRepository
    private val alarmScheduler: AlarmScheduler

    init {
        val dao = NudgeDatabase.getDatabase(application).nudgeDao()
        repository = NudgeRepository(dao)
        alarmScheduler = AlarmScheduler(application)
    }

    val upcomingNudges: StateFlow<List<NudgeEntity>> = repository.upcomingNudges
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _historyFilter = MutableStateFlow(HistoryFilter.LAST_7_DAYS)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    val historyNudges: StateFlow<List<NudgeEntity>> = _historyFilter
        .flatMapLatest { filter ->
            val fromTimestamp = System.currentTimeMillis() - filter.durationMillis
            repository.getHistoryNudges(fromTimestamp)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form states
    var editingNudgeId = MutableStateFlow<Long?>(null)
        private set

    var taskNameInput = MutableStateFlow("")
        private set

    var hourInput = MutableStateFlow("")
        private set

    var minuteInput = MutableStateFlow("")
        private set

    var isPm = MutableStateFlow(false)
        private set

    var isTomorrow = MutableStateFlow(false)
        private set

    init {
        initializeTimeWithDefault()
    }

    private fun initializeTimeWithDefault() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 10) // default 10 mins from now
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val pm = cal.get(Calendar.AM_PM) == Calendar.PM

        hourInput.value = hour12.toString()
        minuteInput.value = String.format("%02d", minute)
        isPm.value = pm
    }

    fun onTaskNameChange(name: String) {
        taskNameInput.value = name
    }

    fun onHourChange(hour: String) {
        val filtered = hour.filter { it.isDigit() }.take(2)
        hourInput.value = filtered
    }

    fun onMinuteChange(minute: String) {
        val filtered = minute.filter { it.isDigit() }.take(2)
        minuteInput.value = filtered
    }

    fun toggleAmPm(toPm: Boolean) {
        isPm.value = toPm
    }

    fun toggleTomorrow(tomorrow: Boolean) {
        isTomorrow.value = tomorrow
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun startEditing(nudge: NudgeEntity) {
        editingNudgeId.value = nudge.id
        taskNameInput.value = nudge.taskName

        val cal = Calendar.getInstance().apply {
            timeInMillis = nudge.scheduledTimeMillis
        }
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val pm = cal.get(Calendar.AM_PM) == Calendar.PM

        hourInput.value = hour12.toString()
        minuteInput.value = String.format("%02d", minute)
        isPm.value = pm

        // Check if scheduled date is tomorrow
        val todayCal = Calendar.getInstance()
        val isDayTomorrow = cal.get(Calendar.DAY_OF_YEAR) != todayCal.get(Calendar.DAY_OF_YEAR) ||
                cal.get(Calendar.YEAR) != todayCal.get(Calendar.YEAR)
        isTomorrow.value = isDayTomorrow
    }

    fun cancelEditing() {
        editingNudgeId.value = null
        taskNameInput.value = ""
        initializeTimeWithDefault()
        isTomorrow.value = false
    }

    fun saveNudge(): Boolean {
        val name = taskNameInput.value.trim()
        if (name.isEmpty()) {
            Toast.makeText(getApplication(), "Enter task name", Toast.LENGTH_SHORT).show()
            return false
        }

        val hour = hourInput.value.toIntOrNull()
        if (hour == null || hour !in 1..12) {
            Toast.makeText(getApplication(), "Hour must be 1 to 12", Toast.LENGTH_SHORT).show()
            return false
        }

        val minute = minuteInput.value.toIntOrNull()
        if (minute == null || minute !in 0..59) {
            Toast.makeText(getApplication(), "Minute must be 00 to 59", Toast.LENGTH_SHORT).show()
            return false
        }

        val targetCal = Calendar.getInstance().apply {
            if (isTomorrow.value) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val hour24 = if (isPm.value) {
                if (hour == 12) 12 else hour + 12
            } else {
                if (hour == 12) 0 else hour
            }
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today and time is already past, automatically roll to tomorrow unless explicitly set
        if (!isTomorrow.value && targetCal.timeInMillis <= System.currentTimeMillis()) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val scheduledTime = targetCal.timeInMillis
        val currentEditId = editingNudgeId.value

        viewModelScope.launch {
            if (currentEditId != null) {
                // Update existing
                val existing = repository.getNudgeById(currentEditId)
                if (existing != null) {
                    val updated = existing.copy(
                        taskName = name,
                        scheduledTimeMillis = scheduledTime,
                        status = NudgeStatus.PENDING.name
                    )
                    repository.update(updated)
                    alarmScheduler.scheduleNudge(updated)
                    Toast.makeText(getApplication(), "Nudge updated", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Insert new
                val newNudge = NudgeEntity(
                    taskName = name,
                    scheduledTimeMillis = scheduledTime,
                    status = NudgeStatus.PENDING.name
                )
                val id = repository.insert(newNudge)
                val insertedWithId = newNudge.copy(id = id)
                alarmScheduler.scheduleNudge(insertedWithId)
                Toast.makeText(getApplication(), "Nudge scheduled", Toast.LENGTH_SHORT).show()
            }

            cancelEditing()
        }
        return true
    }

    fun deleteNudge(nudge: NudgeEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelNudge(nudge.id)
            repository.delete(nudge)
            if (editingNudgeId.value == nudge.id) {
                cancelEditing()
            }
            Toast.makeText(getApplication(), "Nudge deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun testTriggerNow(nudge: NudgeEntity) {
        NudgeQueueManager.enqueue(nudge.id, getApplication())
    }
}
