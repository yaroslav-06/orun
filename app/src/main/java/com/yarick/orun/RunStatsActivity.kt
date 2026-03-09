package com.yarick.orun

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yarick.orun.service.LocationTrackingService
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

        // Collect stats from service
        scope.launch {
            LocationTrackingService.stats.collect { stats ->
                val distanceKm = stats.distanceMeters / 1000f
                tvDistance.text = "%.2f km".format(distanceKm)
                tvCurrentPace.text = if (stats.currentPaceSecPerKm > 0)
                    formatPace(stats.currentPaceSecPerKm) else "--:--"
                tvElevationGain.text = "+%.0f m".format(stats.elevationGainMeters)
                tvElevationLoss.text = "-%.0f m".format(stats.elevationLossMeters)

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
                tvDuration.text = formatDuration(durationSec)
                if (stats.distanceMeters > 0f) {
                    val paceSecPerKm = durationSec * 1000f / stats.distanceMeters
                    tvOverallPace.text = formatPace(paceSecPerKm)
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

    private fun formatDuration(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun formatPace(secPerKm: Float): String {
        val totalSec = secPerKm.toLong()
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d /km".format(m, s)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
