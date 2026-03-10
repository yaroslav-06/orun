package com.yarick.orun

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat
import com.yarick.orun.analysis.BestEffortAnalyzer
import com.yarick.orun.data.LocationPoint
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
            val dao = RunDatabase.getInstance(this@RunDetailActivity).runDao()

            val run = withContext(Dispatchers.IO) {
                dao.getRunById(runId)
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

            // Splits
            val points = withContext(Dispatchers.IO) { dao.getPointsForRun(runId) }
            val splits = computeSplits(points, metric)
            if (splits.isNotEmpty()) {
                findViewById<TextView>(R.id.tvSplitsHeader).visibility = View.VISIBLE
                findViewById<SplitsView>(R.id.splitsView).setSplits(splits, metric)
            }

            // Ensure best effort analysis has been run
            if (!run.isAnalyzed && points.size >= 2) {
                withContext(Dispatchers.IO) {
                    val beDao = RunDatabase.getInstance(this@RunDetailActivity).bestEffortDao()
                    BestEffortAnalyzer.analyze(run, points, beDao, dao)
                }
            }

            // Display best efforts for this run
            val beDao = RunDatabase.getInstance(this@RunDetailActivity).bestEffortDao()
            val efforts = withContext(Dispatchers.IO) { beDao.getByRun(runId) }
            if (efforts.isNotEmpty()) {
                val header = findViewById<TextView>(R.id.tvBestEffortsHeader)
                val container = findViewById<LinearLayout>(R.id.bestEffortsContainer)
                header.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                for (effort in efforts) {
                    val pace = formatPace(effort.distanceMeters.toFloat(), effort.durationMs, metric)

                    val tvDistance = TextView(this@RunDetailActivity)
                    tvDistance.text = effort.distanceKey
                    tvDistance.textSize = 18f
                    TextViewCompat.setTextAppearance(tvDistance, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)

                    val tvStats = TextView(this@RunDetailActivity)
                    tvStats.text = "${formatDuration(effort.durationMs)}  $pace"
                    tvStats.textSize = 14f

                    val row = LinearLayout(this@RunDetailActivity)
                    row.orientation = LinearLayout.VERTICAL
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = (12 * resources.displayMetrics.density).toInt()
                    row.layoutParams = lp

                    row.addView(tvDistance)
                    row.addView(tvStats)
                    container.addView(row)
                }
            }

            btnDelete.isEnabled = true
        }
    }

    private fun computeSplits(points: List<LocationPoint>, metric: Boolean): List<SplitsView.Split> {
        val unitMeters = if (metric) 1000f else 1609.344f
        if (points.size < 2) return emptyList()

        val splits = mutableListOf<SplitsView.Split>()
        var splitIndex = 1
        var distSinceLastSplit = 0f
        var splitStartTime = points.first().timestamp
        var prev = points.first()

        for (point in points.drop(1)) {
            val distResult = FloatArray(1)
            Location.distanceBetween(
                prev.latitude, prev.longitude,
                point.latitude, point.longitude,
                distResult
            )
            val segDist = distResult[0]
            val segDurationMs = point.timestamp - prev.timestamp

            var remainingDist = segDist
            var consumedInSeg = 0f

            while (distSinceLastSplit + remainingDist >= unitMeters) {
                val needDist = unitMeters - distSinceLastSplit
                val frac = if (segDist > 0f) (consumedInSeg + needDist) / segDist else 0f
                val splitEndTime = prev.timestamp + (segDurationMs * frac).toLong()
                val elapsedMs = splitEndTime - splitStartTime
                splits.add(SplitsView.Split(splitIndex++, elapsedMs / 1000f))
                splitStartTime = splitEndTime
                consumedInSeg += needDist
                remainingDist -= needDist
                distSinceLastSplit = 0f
            }
            distSinceLastSplit += remainingDist
            prev = point
        }

        // Partial last split — include if it's at least 0.1 km / 0.1 mi
        val minPartialMeters = if (metric) 100f else 160.934f
        if (distSinceLastSplit >= minPartialMeters) {
            val elapsedMs = prev.timestamp - splitStartTime
            val secPerUnit = elapsedMs * unitMeters / (1000f * distSinceLastSplit)
            splits.add(SplitsView.Split(splitIndex, secPerUnit, distSinceLastSplit))
        }

        return splits
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
