package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NudgeDao {

    @Query("SELECT * FROM nudges WHERE status IN ('PENDING', 'SNOOZED') ORDER BY scheduledTimeMillis ASC")
    fun getUpcomingNudges(): Flow<List<NudgeEntity>>

    @Query("SELECT * FROM nudges WHERE status IN ('PENDING', 'SNOOZED') ORDER BY scheduledTimeMillis ASC")
    suspend fun getUpcomingNudgesList(): List<NudgeEntity>

    @Query("SELECT * FROM nudges WHERE scheduledTimeMillis >= :fromTimestamp ORDER BY scheduledTimeMillis DESC")
    fun getHistoryNudges(fromTimestamp: Long): Flow<List<NudgeEntity>>

    @Query("SELECT * FROM nudges ORDER BY scheduledTimeMillis DESC")
    fun getAllHistoryNudges(): Flow<List<NudgeEntity>>

    @Query("SELECT * FROM nudges WHERE id = :id")
    suspend fun getNudgeById(id: Long): NudgeEntity?

    @Query("SELECT * FROM nudges WHERE status IN ('PENDING', 'SNOOZED') AND scheduledTimeMillis < :currentTime")
    suspend fun getMissedNudges(currentTime: Long): List<NudgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNudge(nudge: NudgeEntity): Long

    @Update
    suspend fun updateNudge(nudge: NudgeEntity)

    @Query("UPDATE nudges SET status = :status, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedAt: Long? = null)

    @Query("UPDATE nudges SET status = 'SNOOZED', scheduledTimeMillis = :newTimeMillis, snoozeCount = snoozeCount + 1 WHERE id = :id")
    suspend fun snoozeNudge(id: Long, newTimeMillis: Long)

    @Query("UPDATE nudges SET status = 'MISSED' WHERE id IN (:ids)")
    suspend fun markAsMissed(ids: List<Long>)

    @Delete
    suspend fun deleteNudge(nudge: NudgeEntity)

    @Query("DELETE FROM nudges WHERE id = :id")
    suspend fun deleteNudgeById(id: Long)
}
