package com.yarick.orun

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
    private var runId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        runId = intent.getLongExtra("run_id", -1L)
        if (runId == -1L) { finish(); return }

        val metric = UnitPreference.isMetric(this)

        val btnDelete = findViewById<Button>(R.id.btnDeleteRun)
        btnDelete.isEnabled = false
        btnDelete.setOnClickListener {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val dao = RunDatabase.getInstance(this@RunDetailActivity).runDao()
                        dao.deletePointsForRun(runId)
                        dao.deleteRunById(runId)
                    }
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete run $runId", e)
                }
            }
        }

        scope.launch {
            val run = withContext(Dispatchers.IO) {
                RunDatabase.getInstance(this@RunDetailActivity).runDao().getRunById(runId)
            } ?: run { finish(); return@launch }

            val endTime = run.endTime ?: run.startTime
            val durationMs = endTime - run.startTime

            val sdf = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
            findViewById<TextView>(R.id.tvDetailDate).text = sdf.format(run.startTime)

            findViewById<TextView>(R.id.tvDetailDistance).text =
                formatDistance(run.totalDistanceMeters, metric)

            findViewById<TextView>(R.id.tvDetailDuration).text = formatDuration(durationMs)

            findViewById<TextView>(R.id.tvDetailPace).text =
                formatPace(run.totalDistanceMeters, durationMs, metric)

            findViewById<TextView>(R.id.tvDetailElevGain).text =
                "Elevation gain: ${formatElevation(run.elevationGainMeters, metric)}"
            findViewById<TextView>(R.id.tvDetailElevLoss).text =
                "Elevation loss: ${formatElevation(run.elevationLossMeters, metric)}"

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

            btnDelete.isEnabled = true
        }
    }

    companion object {
        private const val TAG = "RunDetailActivity"
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
