package com.yarick.orun.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RunDao {
    @Insert suspend fun insertRun(run: Run): Long
    @Insert suspend fun insertPoint(point: LocationPoint)
    @Query("SELECT * FROM location_points WHERE runId = :runId ORDER BY timestamp")
    suspend fun getPointsForRun(runId: Long): List<LocationPoint>
}
