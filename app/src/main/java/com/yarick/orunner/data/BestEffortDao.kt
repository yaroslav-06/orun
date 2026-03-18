package com.yarick.orunner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BestEffortDao {
    @Insert
    suspend fun insert(effort: BestEffort)

    @Delete
    suspend fun delete(effort: BestEffort)

    @Query("SELECT * FROM best_efforts WHERE distanceKey = :key ORDER BY durationMs ASC")
    suspend fun getByDistance(key: String): List<BestEffort>

    @Query("SELECT * FROM best_efforts WHERE runId = :runId ORDER BY distanceMeters DESC")
    suspend fun getByRun(runId: Long): List<BestEffort>

    @Query("""
        SELECT * FROM best_efforts AS be
        WHERE id = (
            SELECT id FROM best_efforts
            WHERE distanceKey = be.distanceKey
            ORDER BY durationMs ASC LIMIT 1
        )
        ORDER BY distanceMeters DESC
    """)
    suspend fun getAllBestPerDistance(): List<BestEffort>
}
