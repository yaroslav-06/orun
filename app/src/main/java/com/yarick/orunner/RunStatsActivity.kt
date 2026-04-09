package com.yarick.orunner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yarick.orunner.data.RunStats
import com.yarick.orunner.service.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RunStatsActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var frozenDurationSec: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_stats)

        val metric = UnitPreference.isMetric(this)

        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvOverallPace = findViewById<TextView>(R.id.tvOverallPace)
        val tvCurrentPace = findViewById<TextView>(R.id.tvCurrentPace)
        val tvElevationGain = findViewById<TextView>(R.id.tvElevationGain)
        val tvElevationLoss = findViewById<TextView>(R.id.tvElevationLoss)
        val btnStop = findViewById<Button>(R.id.btnStop)

        val llGoalProgress = findViewById<View>(R.id.llGoalProgress)
        val pbGoalProgress = findViewById<ProgressBar>(R.id.pbGoalProgress)
        val tvGoalPercent = findViewById<TextView>(R.id.tvGoalPercent)
        val tvGoalEta = findViewById<TextView>(R.id.tvGoalEta)

        val goalDistanceMeters: Float? =
            if (intent.hasExtra(RunGoalSetupActivity.EXTRA_GOAL_DISTANCE_METERS))
                intent.getFloatExtra(RunGoalSetupActivity.EXTRA_GOAL_DISTANCE_METERS, 0f)
            else null
        val goalDurationMs: Long? =
            if (intent.hasExtra(RunGoalSetupActivity.EXTRA_GOAL_DURATION_MS))
                intent.getLongExtra(RunGoalSetupActivity.EXTRA_GOAL_DURATION_MS, 0L)
            else null
        val hasGoal = goalDistanceMeters != null || goalDurationMs != null
        if (hasGoal) llGoalProgress.visibility = View.VISIBLE

        // Collect stats from service
        scope.launch {
            LocationTrackingService.stats.collect { stats ->
                tvDistance.text = formatDistance(stats.distanceMeters, metric)
                tvCurrentPace.text = if (stats.currentPaceSecPerKm > 0)
                    formatPaceFromSecPerKm(stats.currentPaceSecPerKm, metric) else "--:--"
                tvElevationGain.text = "+${formatElevation(stats.elevationGainMeters, metric)}"
                tvElevationLoss.text = "-${formatElevation(stats.elevationLossMeters, metric)}"

                if (stats.isFinished) {
                    if (frozenDurationSec == null && stats.startTime > 0) {
                        frozenDurationSec = (System.currentTimeMillis() - stats.startTime) / 1000
                    }
                    btnStop.isEnabled = false
                }
            }
        }

        // Duration ticker — also computes overall pace and goal progress
        scope.launch {
            while (true) {
                val stats = LocationTrackingService.stats.value
                val elapsedMs = if (stats.startTime > 0) System.currentTimeMillis() - stats.startTime else 0L
                val durationSec = frozenDurationSec ?: (elapsedMs / 1000L)
                tvDuration.text = formatDurationSec(durationSec)
                if (stats.distanceMeters > 0f) {
                    tvOverallPace.text = formatPace(stats.distanceMeters, durationSec * 1000L, metric)
                } else {
                    tvOverallPace.text = "--:--"
                }
                if (hasGoal) {
                    updateGoalProgress(
                        stats = stats,
                        elapsedMs = elapsedMs,
                        goalDistMeters = goalDistanceMeters,
                        goalDurMs = goalDurationMs,
                        pbGoalProgress = pbGoalProgress,
                        tvGoalPercent = tvGoalPercent,
                        tvGoalEta = tvGoalEta
                    )
                }
                if (frozenDurationSec != null) break
                delay(1000)
            }
        }

        btnStop.setOnClickListener {
            startService(Intent(this, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            })
        }
    }

    private fun updateGoalProgress(
        stats: RunStats,
        elapsedMs: Long,
        goalDistMeters: Float?,
        goalDurMs: Long?,
        pbGoalProgress: ProgressBar,
        tvGoalPercent: TextView,
        tvGoalEta: TextView
    ) {
        val (fraction, etaText) = when {
            goalDistMeters != null && goalDistMeters > 0f -> {
                val progress = (stats.distanceMeters / goalDistMeters).coerceIn(0f, 1f)
                val remainingMeters = (goalDistMeters - stats.distanceMeters).coerceAtLeast(0f)
                val eta = when {
                    remainingMeters <= 0f -> "Goal reached!"
                    stats.currentPaceSecPerKm > 0f -> {
                        val remainingSec = (stats.currentPaceSecPerKm * remainingMeters / 1000f).toLong()
                        "~${formatDurationSec(remainingSec)} remaining"
                    }
                    else -> "ETA: --"
                }
                progress to eta
            }
            goalDurMs != null && goalDurMs > 0L -> {
                val progress = (elapsedMs.toFloat() / goalDurMs).coerceIn(0f, 1f)
                val remainingMs = (goalDurMs - elapsedMs).coerceAtLeast(0L)
                val eta = if (remainingMs <= 0L) "Goal reached!"
                          else "~${formatDurationSec(remainingMs / 1000L)} remaining"
                progress to eta
            }
            else -> 0f to ""
        }

        pbGoalProgress.progress = (fraction * 100).toInt().coerceIn(0, 100)
        tvGoalPercent.text = "${(fraction * 100).toInt().coerceIn(0, 100)}% complete"
        tvGoalEta.text = etaText
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
