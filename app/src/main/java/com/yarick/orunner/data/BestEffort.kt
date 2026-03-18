package com.yarick.orunner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "best_efforts", indices = [Index("runId")])
data class BestEffort(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val distanceKey: String,
    val distanceMeters: Double,
    val runId: Long,
    val durationMs: Long
)
