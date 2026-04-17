package com.yarick.orunner

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
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

        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvOverallPace = findViewById<TextView>(R.id.tvOverallPace)
        val tvCurrentPace = findViewById<TextView>(R.id.tvCurrentPace)
        val tvElevationGain = findViewById<TextView>(R.id.tvElevationGain)
        val tvElevationLoss = findViewById<TextView>(R.id.tvElevationLoss)
        val btnStop = findViewById<Button>(R.id.btnStop)

        val llGoalProgress = findViewById<View>(R.id.llGoalProgress)
        val tvGoalPercent = findViewById<TextView>(R.id.tvGoalPercent)
        val tvGoalEta = findViewById<TextView>(R.id.tvGoalEta)
        val tvGoalEtaLabel = findViewById<TextView>(R.id.tvGoalEtaLabel)

        val goalDistanceMeters: Float? =
            if (intent.hasExtra(RunGoalSetupActivity.EXTRA_GOAL_DISTANCE_METERS))
                intent.getFloatExtra(RunGoalSetupActivity.EXTRA_GOAL_DISTANCE_METERS, 0f)
            else null
        val goalDurationMs: Long? =
            if (intent.hasExtra(RunGoalSetupActivity.EXTRA_GOAL_DURATION_MS))
                intent.getLongExtra(RunGoalSetupActivity.EXTRA_GOAL_DURATION_MS, 0L)
            else null
        val hasGoal = goalDistanceMeters != null || goalDurationMs != null
        if (hasGoal) {
            llGoalProgress.visibility = View.VISIBLE
            if (goalDurationMs != null) tvGoalEtaLabel.text = "Est. distance"
        }

        // Collect stats from service
        scope.launch {
            LocationTrackingService.stats.collect { stats ->
                val metric = UnitPreference.isMetric(this@RunStatsActivity)
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
                val metric = UnitPreference.isMetric(this@RunStatsActivity)
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
        tvGoalPercent: TextView,
        tvGoalEta: TextView
    ) {
        val (fraction, etaText) = when {
            goalDistMeters != null && goalDistMeters > 0f -> {
                val progress = (stats.distanceMeters / goalDistMeters).coerceIn(0f, 1f)
                val remainingMeters = (goalDistMeters - stats.distanceMeters).coerceAtLeast(0f)
                val eta = when {
                    remainingMeters <= 0f -> "Reached!"
                    stats.currentPaceSecPerKm > 0f -> {
                        val remainingSec = (stats.currentPaceSecPerKm * remainingMeters / 1000f).toLong()
                        "~${formatDurationSec(elapsedMs / 1000L + remainingSec)}"
                    }
                    else -> "--"
                }
                progress to eta
            }
            goalDurMs != null && goalDurMs > 0L -> {
                val progress = (elapsedMs.toFloat() / goalDurMs).coerceIn(0f, 1f)
                val remainingMs = (goalDurMs - elapsedMs).coerceAtLeast(0L)
                val eta = when {
                    remainingMs <= 0L -> "Reached!"
                    stats.currentPaceSecPerKm > 0f -> {
                        val remainingSec = remainingMs / 1000f
                        val estMeters = remainingSec / stats.currentPaceSecPerKm * 1000f
                        "~${"%.2f".format((stats.distanceMeters + estMeters) / 1000f)} km"
                    }
                    else -> "--"
                }
                progress to eta
            }
            else -> 0f to "--"
        }

        tvGoalPercent.text = "${(fraction * 100).toInt().coerceIn(0, 100)}%"
        tvGoalEta.text = etaText
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
