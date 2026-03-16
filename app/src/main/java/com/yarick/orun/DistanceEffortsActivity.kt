package com.yarick.orun

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yarick.orun.data.RunDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DistanceEffortsActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_distance_efforts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val distanceKey = intent.getStringExtra("distance_key") ?: run { finish(); return }
        val distanceLabel = intent.getStringExtra("distance_label") ?: distanceKey
        title = distanceLabel

        val rv = findViewById<RecyclerView>(R.id.rvEfforts)
        rv.layoutManager = LinearLayoutManager(this)

        val metric = UnitPreference.isMetric(this)

        scope.launch {
            val db = RunDatabase.getInstance(this@DistanceEffortsActivity)
            val beDao = db.bestEffortDao()
            val runDao = db.runDao()

            val efforts = withContext(Dispatchers.IO) { beDao.getByDistance(distanceKey) }
            val items = withContext(Dispatchers.IO) {
                efforts.mapNotNull { effort ->
                    val run = runDao.getRunById(effort.runId) ?: return@mapNotNull null
                    Pair(effort, run.startTime)
                }
            }

            rv.adapter = EffortAdapter(items, metric) { runId ->
                startActivity(
                    Intent(this@DistanceEffortsActivity, RunDetailActivity::class.java)
                        .putExtra("run_id", runId)
                )
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
