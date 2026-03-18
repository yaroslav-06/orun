package com.yarick.orunner.analysis

import android.location.Location
import com.yarick.orunner.data.BestEffort
import com.yarick.orunner.data.BestEffortDao
import com.yarick.orunner.data.LocationPoint
import com.yarick.orunner.data.Run
import com.yarick.orunner.data.RunDao

object BestEffortAnalyzer {

    val DISTANCES = listOf(
        "1k"            to 1_000.0,
        "1mi"           to 1_609.344,
        "5k"            to 5_000.0,
        "5mi"           to 8_046.72,
        "10k"           to 10_000.0,
        "10mi"          to 16_093.44,
        "20k"           to 20_000.0,
        "20mi"          to 32_186.88,
        "Half Marathon" to 21_097.5,
        "30k"           to 30_000.0,
        "25mi"          to 40_233.6,
        "40k"           to 40_000.0,
        "Marathon"      to 42_195.0,
        "50k"           to 50_000.0,
        "50mi"          to 80_467.2,
        "75k"           to 75_000.0,
        "75mi"          to 120_700.8,
        "100k"          to 100_000.0,
        "100mi"         to 160_934.4,
    )

    suspend fun analyze(
        run: Run,
        points: List<LocationPoint>,
        dao: BestEffortDao,
        runDao: RunDao
    ) {
        if (points.size < 2) {
            runDao.updateRun(run.copy(isAnalyzed = true))
            return
        }

        val n = points.size
        val cumDist = DoubleArray(n)
        val timestamps = LongArray(n) { points[it].timestamp }
        for (i in 1 until n) {
            val result = FloatArray(1)
            Location.distanceBetween(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude,
                result
            )
            cumDist[i] = cumDist[i - 1] + result[0]
        }

        val totalDist = cumDist[n - 1]

        for ((key, targetDist) in DISTANCES) {
            if (totalDist < targetDist) continue

            var bestMs = Long.MAX_VALUE
            var left = 0
            var right = 0

            while (right < n) {
                val gap = cumDist[right] - cumDist[left]
                if (gap >= targetDist) {
                    // Interpolate exact crossing time within the segment ending at right
                    val lastSegLen = cumDist[right] - cumDist[right - 1]
                    val overshoot = gap - targetDist
                    val frac = if (lastSegLen > 0) (lastSegLen - overshoot) / lastSegLen else 1.0
                    val endTimeMs = timestamps[right - 1] + (frac * (timestamps[right] - timestamps[right - 1])).toLong()
                    val elapsedMs = endTimeMs - timestamps[left]
                    if (elapsedMs > 0 && elapsedMs < bestMs) {
                        bestMs = elapsedMs
                    }
                    left++
                } else {
                    right++
                }
            }

            if (bestMs == Long.MAX_VALUE) continue

            dao.insert(BestEffort(distanceKey = key, distanceMeters = targetDist, runId = run.id, durationMs = bestMs))
        }

        runDao.updateRun(run.copy(isAnalyzed = true))
    }
}
