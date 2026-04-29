package com.example.pillmate.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pillmate.data.local.dao.MedicationDao
import com.example.pillmate.data.local.entity.MedicationEntity
import com.example.pillmate.data.local.dao.SupplyLogDao
import com.example.pillmate.data.local.entity.SupplyLogEntity
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ProfileEntity
import com.example.pillmate.data.local.entity.SavedAccountEntity

@Database(entities = [MedicationEntity::class, SupplyLogEntity::class, ProfileEntity::class, SavedAccountEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun supplyLogDao(): SupplyLogDao
    abstract fun profileDao(): ProfileDao

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
