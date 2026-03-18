package com.yarick.orunner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class Run(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistanceMeters: Float = 0f,
    val elevationGainMeters: Float = 0f,
    val elevationLossMeters: Float = 0f,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val isAnalyzed: Boolean = false
)
