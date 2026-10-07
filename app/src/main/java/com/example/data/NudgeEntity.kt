package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NudgeStatus {
    PENDING,
    COMPLETED,
    MISSED,
    SNOOZED
}

@Entity(tableName = "nudges")
data class NudgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskName: String,
    val scheduledTimeMillis: Long,
    val status: String = NudgeStatus.PENDING.name,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null,
    val snoozeCount: Int = 0
)
