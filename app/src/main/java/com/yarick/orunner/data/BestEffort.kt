package com.yarick.orunner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "best_efforts",
    indices = [Index("runId")],
    foreignKeys = [ForeignKey(
        entity = Run::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BestEffort(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val distanceKey: String,
    val distanceMeters: Double,
    val runId: Long,
    val durationMs: Long
)
