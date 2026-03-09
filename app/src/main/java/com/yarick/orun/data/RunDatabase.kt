package com.yarick.orun.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Run::class, LocationPoint::class], version = 2, exportSchema = false)
abstract class RunDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao

    companion object {
        @Volatile
        private var INSTANCE: RunDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE runs ADD COLUMN totalDistanceMeters REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE runs ADD COLUMN elevationGainMeters REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE runs ADD COLUMN elevationLossMeters REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE runs ADD COLUMN avgHeartRate INTEGER")
                database.execSQL("ALTER TABLE runs ADD COLUMN maxHeartRate INTEGER")
                database.execSQL("ALTER TABLE location_points ADD COLUMN heartRate INTEGER")
                database.execSQL("ALTER TABLE location_points ADD COLUMN cadence INTEGER")
            }
        }

        fun getInstance(context: Context): RunDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RunDatabase::class.java,
                    "run_database"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
