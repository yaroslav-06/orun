package com.yarick.orun.data

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

    @Query("SELECT * FROM best_efforts WHERE runId = :runId ORDER BY distanceMeters ASC")
    suspend fun getByRun(runId: Long): List<BestEffort>
}
