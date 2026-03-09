package com.yarick.orun.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert suspend fun insertRun(run: Run): Long
    @Insert suspend fun insertPoint(point: LocationPoint)
    @Update suspend fun updateRun(run: Run)
    @Query("SELECT * FROM location_points WHERE runId = :runId ORDER BY timestamp")
    suspend fun getPointsForRun(runId: Long): List<LocationPoint>
    @Query("SELECT * FROM runs WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getAllFinishedRuns(): Flow<List<Run>>
    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRunById(id: Long): Run?
}
