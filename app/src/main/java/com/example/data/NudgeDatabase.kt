package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NudgeEntity::class], version = 1, exportSchema = false)
abstract class NudgeDatabase : RoomDatabase() {

    abstract fun nudgeDao(): NudgeDao

    companion object {
        @Volatile
        private var INSTANCE: NudgeDatabase? = null

        fun getDatabase(context: Context): NudgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NudgeDatabase::class.java,
                    "nudge_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
