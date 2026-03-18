package com.yarick.orunner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        // Duration ticker — also computes overall pace
        scope.launch {
            while (true) {
                val stats = LocationTrackingService.stats.value
                val durationSec = frozenDurationSec
                    ?: if (stats.startTime > 0) (System.currentTimeMillis() - stats.startTime) / 1000 else 0L
                tvDuration.text = formatDurationSec(durationSec)
                if (stats.distanceMeters > 0f) {
                    tvOverallPace.text = formatPace(stats.distanceMeters, durationSec * 1000L, metric)
                } else {
                    tvOverallPace.text = "--:--"
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

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
