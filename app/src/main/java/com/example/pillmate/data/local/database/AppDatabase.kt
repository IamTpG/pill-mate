package com.example.pillmate.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pillmate.data.local.dao.ChatDao
import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ChatMessageEntity
import com.example.pillmate.data.local.entity.ChatSessionEntity
import com.example.pillmate.data.local.entity.ProfileEntity
import com.example.pillmate.data.local.entity.SavedAccountEntity
import com.example.pillmate.data.local.entity.MedicalImageEntity
import com.example.pillmate.data.local.entity.ScheduleEntity

@Database(
    entities = [
        MedicationEntity::class,
        SupplyLogEntity::class,
        ProfileEntity::class,
        SavedAccountEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MedicalImageEntity::class,
        ScheduleEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun supplyLogDao(): SupplyLogDao
    abstract fun profileDao(): ProfileDao
    abstract fun chatDao(): ChatDao
    abstract fun imageDao(): com.example.pillmate.data.local.dao.ImageDao
    abstract fun scheduleDao(): com.example.pillmate.data.local.dao.ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pillmate_database"
                )
                .fallbackToDestructiveMigration() // Useful for dev: wipes DB on version change
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
