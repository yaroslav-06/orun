package com.yarick.orun

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yarick.orun.data.RunDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class RunDetailActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val runId = intent.getLongExtra("run_id", -1L)
        if (runId == -1L) { finish(); return }

        scope.launch {
            val run = withContext(Dispatchers.IO) {
                RunDatabase.getInstance(this@RunDetailActivity).runDao().getRunById(runId)
            } ?: run { finish(); return@launch }

            val endTime = run.endTime ?: run.startTime
            val durationMs = endTime - run.startTime

            val sdf = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
            findViewById<TextView>(R.id.tvDetailDate).text = sdf.format(run.startTime)

            findViewById<TextView>(R.id.tvDetailDistance).text =
                "%.2f km".format(run.totalDistanceMeters / 1000f)

            val totalSec = durationMs / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            findViewById<TextView>(R.id.tvDetailDuration).text =
                if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)

            val paceStr = if (run.totalDistanceMeters > 0f) {
                val secPerKm = (durationMs / 1000f) / (run.totalDistanceMeters / 1000f)
                val pm = (secPerKm / 60).toInt()
                val ps = (secPerKm % 60).toInt()
                "%d:%02d /km".format(pm, ps)
            } else "– /km"
            findViewById<TextView>(R.id.tvDetailPace).text = paceStr

            findViewById<TextView>(R.id.tvDetailElevGain).text =
                "Elevation gain: %.0f m".format(run.elevationGainMeters)
            findViewById<TextView>(R.id.tvDetailElevLoss).text =
                "Elevation loss: %.0f m".format(run.elevationLossMeters)

            run.avgHeartRate?.let {
                val tv = findViewById<TextView>(R.id.tvDetailAvgHR)
                tv.text = "Avg heart rate: $it bpm"
                tv.visibility = View.VISIBLE
            }
            run.maxHeartRate?.let {
                val tv = findViewById<TextView>(R.id.tvDetailMaxHR)
                tv.text = "Max heart rate: $it bpm"
                tv.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
