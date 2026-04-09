package com.yarick.orunner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RunGoalSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GOAL_DISTANCE_METERS = "extra_goal_distance_meters"
        const val EXTRA_GOAL_DURATION_MS = "extra_goal_duration_ms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_goal_setup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val metric = UnitPreference.isMetric(this)
        findViewById<TextView>(R.id.tvDistanceUnit).text = if (metric) "km" else "mi"

        findViewById<Button>(R.id.btnStartRun).setOnClickListener {
            val result = Intent()

            val distText = findViewById<EditText>(R.id.etDistanceGoal).text.toString().trim()
            if (distText.isNotEmpty()) {
                val distUserUnit = distText.toFloatOrNull()
                if (distUserUnit != null && distUserUnit > 0f) {
                    val meters = if (metric) distUserUnit * 1000f else distUserUnit * 1609.344f
                    result.putExtra(EXTRA_GOAL_DISTANCE_METERS, meters)
                }
            }

            val durText = findViewById<EditText>(R.id.etDurationGoal).text.toString().trim()
            if (durText.isNotEmpty()) {
                val minutes = durText.toIntOrNull()
                if (minutes != null && minutes > 0) {
                    result.putExtra(EXTRA_GOAL_DURATION_MS, minutes * 60_000L)
                }
            }

            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
