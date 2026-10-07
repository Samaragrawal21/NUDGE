package com.example.data

import kotlinx.coroutines.flow.Flow

class NudgeRepository(private val dao: NudgeDao) {

    val upcomingNudges: Flow<List<NudgeEntity>> = dao.getUpcomingNudges()

    fun getHistoryNudges(fromTimestamp: Long): Flow<List<NudgeEntity>> {
        return dao.getHistoryNudges(fromTimestamp)
    }

    suspend fun getUpcomingNudgesList(): List<NudgeEntity> {
        return dao.getUpcomingNudgesList()
    }

    suspend fun getNudgeById(id: Long): NudgeEntity? {
        return dao.getNudgeById(id)
    }

    suspend fun getMissedNudges(currentTime: Long): List<NudgeEntity> {
        return dao.getMissedNudges(currentTime)
    }

    suspend fun insert(nudge: NudgeEntity): Long {
        return dao.insertNudge(nudge)
    }

    suspend fun update(nudge: NudgeEntity) {
        dao.updateNudge(nudge)
    }

    suspend fun markCompleted(id: Long) {
        dao.updateStatus(id, NudgeStatus.COMPLETED.name, System.currentTimeMillis())
    }

    suspend fun snooze(id: Long, newTimeMillis: Long) {
        dao.snoozeNudge(id, newTimeMillis)
    }

    suspend fun markMissed(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            dao.markAsMissed(ids)
        }
    }

    suspend fun delete(nudge: NudgeEntity) {
        dao.deleteNudge(nudge)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteNudgeById(id)
    }
}
